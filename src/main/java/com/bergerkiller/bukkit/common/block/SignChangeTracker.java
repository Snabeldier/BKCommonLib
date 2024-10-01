package com.bergerkiller.bukkit.common.block;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.internal.CommonCapabilities;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.generated.org.bukkit.block.SignHandle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.Logging;
import com.bergerkiller.bukkit.common.conversion.type.HandleConversion;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.generated.net.minecraft.world.level.WorldHandle;
import com.bergerkiller.generated.net.minecraft.world.level.block.entity.TileEntitySignHandle;
import com.bergerkiller.generated.org.bukkit.craftbukkit.block.CraftBlockHandle;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import com.bergerkiller.mountiplex.reflection.util.FastField;

/**
 * Efficiently detects when the text contents of a Sign change, when the
 * sign's backing block entity unloads and re-loads, and when the sign
 * block is destroyed in the world.
 *
 * Detecting will actively load the chunk the sign is in.
 */
public class SignChangeTracker implements Cloneable, SignLineAccessor {
    private final Block block;
    private Sign state;
    private BlockData blockData;
    private TileEntitySignHandle tileEntity;
    private Object[] lastRawFrontLines;
    private Object[] lastRawBackLines;
    private String[] lastMessageFrontLines;
    private String[] lastMessageBackLines;

    // Need to swap what implementation we're using for certain server types
    private static final Function<Block, SignChangeTracker> constructor;
    static {
        Function<Block, SignChangeTracker> constr = SignChangeTracker::new; // Default
        if (Common.evaluateMCVersion("<=", "1.12.2") && Common.SERVER.isForgeServer()) {
            try {
                final FastField<List<Object>> tileEntityListField = new FastField<>();
                tileEntityListField.init(Resolver.resolveAndGetDeclaredField(WorldHandle.T.getType(), "tileEntityList"));
                constr = block -> {
                    List<Object> worldTileEntities = tileEntityListField.get(HandleConversion.toWorldHandle(block.getWorld()));
                    return new SignChangeTrackerMohistLegacy(block, worldTileEntities);
                };
            } catch (Throwable t) {
                Logging.LOGGER_REFLECTION.log(Level.SEVERE, "[Mohist Compat] Failed to find World tileEntityList field", t);
            }
        }
        constructor = constr;
    }

    protected SignChangeTracker(Block block) {
        this.block = block;
        this.state = null;
        this.resetTileEntity();
    }

    private void initState(Sign state) {
        this.state = state;
        this.loadTileEntity(TileEntitySignHandle.fromBukkit(state));
    }

    private void loadTileEntity(TileEntitySignHandle tile) {
        if (tile == null) {
            this.resetTileEntity();
        } else {
            this.tileEntity = tile;
            this.lastRawFrontLines = tile.getRawFrontLines().clone();
            this.lastRawBackLines = CommonCapabilities.HAS_SIGN_BACK_TEXT
                    ? tile.getRawBackLines().clone() : null;
            this.lastMessageFrontLines = null;
            this.lastMessageBackLines = null;

            // Note: it is possible for a TileEntity to be retrieved while it's added to a Chunk,
            // but not yet to a World. Especially on 1.12.2 and before. For that reason, we got to
            // check whether the World was assigned to the tile entity. If not, we cannot use the tile
            // entity's property method, as it throws a NPE.
            this.blockData = this.isTileRemoved(tile) ? WorldUtil.getBlockData(this.block) : tile.getBlockData();
        }
    }

    private void resetTileEntity() {
        this.tileEntity = null;
        this.lastRawFrontLines = null;
        this.lastRawBackLines = null;
        this.lastMessageFrontLines = null;
        this.lastMessageBackLines = null;
        this.blockData = null;
    }

    /**
     * Gets the World the Sign is on
     *
     * @return World
     */
    public World getWorld() {
        return this.block.getWorld();
    }

    /**
     * Gets the X-coordinate where the Sign is located
     *
     * @return Sign location X-coordinate
     */
    public int getX() {
        return this.block.getX();
    }

    /**
     * Gets the Y-coordinate where the Sign is located
     *
     * @return Sign location Y-coordinate
     */
    public int getY() {
        return this.block.getY();
    }

    /**
     * Gets the Z-coordinate where the Sign is located
     *
     * @return Sign location Z-coordinate
     */
    public int getZ() {
        return this.block.getZ();
    }

    @Override
    public String getLine(SignSide side, int index) {
        return side.getLine(this, index);
    }

    @Override
    public void setLine(SignSide side, int index, String text) {
        side.setLine(this, index, text);
    }

    @Override
    public String[] getLines(SignSide side) {
        return side.getLines(this);
    }

    @Override
    public String getFrontLine(int index) {
        return getFrontLines()[index];
    }

    @Override
    public void setFrontLine(int index, String text) {
        checkRemoved();
        if (lastMessageFrontLines != null) {
            lastMessageFrontLines[index] = text;
        }
        SignHandle.T.setFrontLine.invoke(state, index, text);
        state.update(true);
    }

    @Override
    public String[] getFrontLines() {
        checkRemoved();
        String[] lines = lastMessageFrontLines;
        if (lines == null) {
            lastMessageFrontLines = lines = this.tileEntity.getMessageFrontLines();
        }
        return lines;
    }

    @Override
    public ChatText getFormattedFrontLine(int index) {
        checkRemoved();
        return ChatText.fromComponent(tileEntity.getRawFrontLines()[index]);
    }

    @Override
    public void setFormattedFrontLine(int index, ChatText text) {
        checkRemoved();
        this.tileEntity.setFormattedFrontLine(index, text);

        // Must recreate sign state, as that one caches the sign lines which are now outdated
        this.lastMessageFrontLines = null;
        this.state = this.tileEntity.toBukkit();
    }

    @Override
    public ChatText[] getFormattedFrontLines() {
        checkRemoved();
        return LogicUtil.mapArray(tileEntity.getRawFrontLines(), ChatText.class, ChatText::fromComponent);
    }

    @Override
    public String getBackLine(int index) {
        return getBackLines()[index];
    }

    @Override
    public void setBackLine(int index, String text) {
        checkRemoved();
        if (CommonCapabilities.HAS_SIGN_BACK_TEXT) {
            if (lastMessageBackLines != null) {
                lastMessageBackLines[index] = text;
            }
            SignHandle.T.setBackLine.invoke(state, index, text);
            state.update(true);
        }
    }

    @Override
    public String[] getBackLines() {
        checkRemoved();
        String[] lines = lastMessageBackLines;
        if (lines == null) {
            lastMessageBackLines = lines = this.tileEntity.getMessageBackLines();
        }
        return lines;
    }

    @Override
    public ChatText getFormattedBackLine(int index) {
        checkRemoved();
        return ChatText.fromComponent(tileEntity.getRawBackLines()[index]);
    }

    @Override
    public void setFormattedBackLine(int index, ChatText text) {
        checkRemoved();
        if (CommonCapabilities.HAS_SIGN_BACK_TEXT) {
            this.tileEntity.setFormattedBackLine(index, text);

            // Must recreate sign state, as that one caches the sign lines which are now outdated
            this.lastMessageBackLines = null;
            this.state = this.tileEntity.toBukkit();
        }
    }

    @Override
    public ChatText[] getFormattedBackLines() {
        checkRemoved();
        return LogicUtil.mapArray(tileEntity.getRawBackLines(), ChatText.class, ChatText::fromComponent);
    }

    /**
     * Returns a sign line accessor that automatically calls {@link #update()} for all method
     * calls that retrieve lines. This effectively shows a live view of the text on the sign.
     *
     * @return Live-updating sign lines accessor
     */
    public SignLineAccessor liveUpdatingAccessor() {
        return new SignLineAccessor() {
            @Override
            public String getLine(SignSide side, int index) {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getLine(side, index);
            }

            @Override
            public void setLine(SignSide side, int index, String text) {
                SignChangeTracker.this.setLine(side, index, text);
            }

            @Override
            public String[] getLines(SignSide side) {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getLines(side);
            }

            @Override
            public String getFrontLine(int index) {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getFrontLine(index);
            }

            @Override
            public void setFrontLine(int index, String text) {
                SignChangeTracker.this.setFrontLine(index, text);
            }

            @Override
            public String[] getFrontLines() {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getFrontLines();
            }

            @Override
            public ChatText getFormattedFrontLine(int index) {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getFormattedFrontLine(index);
            }

            @Override
            public void setFormattedFrontLine(int index, ChatText text) {
                SignChangeTracker.this.setFormattedFrontLine(index, text);
            }

            @Override
            public ChatText[] getFormattedFrontLines() {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getFormattedFrontLines();
            }

            @Override
            public String getBackLine(int index) {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getBackLine(index);
            }

            @Override
            public void setBackLine(int index, String text) {
                SignChangeTracker.this.setBackLine(index, text);
            }

            @Override
            public String[] getBackLines() {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getBackLines();
            }

            @Override
            public ChatText getFormattedBackLine(int index) {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getFormattedBackLine(index);
            }

            @Override
            public void setFormattedBackLine(int index, ChatText text) {
                SignChangeTracker.this.setFormattedBackLine(index, text);
            }

            @Override
            public ChatText[] getFormattedBackLines() {
                SignChangeTracker.this.update();
                return SignChangeTracker.this.getFormattedBackLines();
            }
        };
    }

    /**
     * Gets the BlockFace the Sign is attached to
     *
     * @return Sign attached face
     */
    public BlockFace getAttachedFace() {
        checkRemoved();
        return this.blockData.getAttachedFace();
    }

    /**
     * Gets the BlockFace the Sign faces. For sign posts, this is the rotation.
     * For wall signs, it is the opposite of {@link #getAttachedFace()}.
     *
     * @return Sign facing
     */
    public BlockFace getFacing() {
        checkRemoved();
        return this.blockData.getFacingDirection();
    }

    /**
     * Gets whether the Sign is attached to a particular Block
     *
     * @param block Block to check
     * @return True if the Sign is attached to the Block
     * @see #getAttachedFace()
     */
    public boolean isAttachedTo(Block block) {
        Block signBlock = this.getBlock();
        BlockFace attachedFace = this.getAttachedFace();
        return (block.getX() - signBlock.getX()) == attachedFace.getModX() &&
               (block.getY() - signBlock.getY()) == attachedFace.getModY() &&
               (block.getZ() - signBlock.getZ()) == attachedFace.getModZ();
    }

    /**
     * Returns true if the sign was broken/removed from the World. If true, then
     * {@link #getSign()} will return null
     *
     * @return True if removed
     */
    public boolean isRemoved() {
        return this.state == null;
    }

    /**
     * Gets the Block where the sign is at
     *
     * @return Sign Block
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * Gets the BlockData of the sign. Returns null if {@link #isRemoved()}.
     * Only {@link #update()} updates the return value of this method.
     *
     * @return Sign Block Data
     */
    public BlockData getBlockData() {
        return this.blockData;
    }

    /**
     * Gets a live-updated Sign instance.
     * Only {@link #update()} updates the return value of this method.<br>
     * <br>
     * This object should <b>not</b> be used for getting and setting String text,
     * because this is very laggy on Paper and partially broken on Spigot.
     *
     * @return Sign
     */
    public Sign getSign() {
        return this.state;
    }

    /**
     * Checks the sign state to see if changes have occurred. If the sign Block Entity
     * reloaded, or the sign text contents changed, true is returned. If there were
     * no changes then false is returned.<br>
     * <br>
     * If the sign was removed entirely, then true is also returned, which should be
     * checked with {@link #isRemoved()}. When removed, the {@link #getSign()} will
     * return null.
     *
     * @return True if changes occurred, or the sign was removed
     */
    public boolean update() {
        TileEntitySignHandle tileEntity = this.tileEntity;

        // Check world to see if a tile entity now exists at this Block.
        // Reading tile entities is slow, so avoid doing that if we can.
        if (tileEntity == null) {
            return tryLoadFromWorld(); // If found, initializes and returns true
        }

        // Ask the TileEntity we already have whether it was removed from the World
        // If it was, we must re-set and re-check for the sign.
        if (isTileRemoved(tileEntity)) {
            if (tryLoadFromWorld()) {
                return true; // Backing TileEntity instance changed, so probably changed
            } else {
                this.state = null;
                this.resetTileEntity();
                return true; // Sign is gone
            }
        }

        // Check when BlockData changes. This is when a sign is rotated, or changed from wall sign to sign post
        boolean blockDataChanged = false;
        {
            Object newBlockDataRaw = tileEntity.getRawBlockData();
            if (newBlockDataRaw != this.blockData.getData()) {
                this.blockData = BlockData.fromBlockData(newBlockDataRaw);
                blockDataChanged = true;
            }
        }

        // Check for sign lines that change. For this, we check the internal IChatBaseComponent contents
        return detectChangedLines(tileEntity) || blockDataChanged;
    }

    private void checkRemoved() {
        if (isRemoved()) {
            if (block == null) {
                throw new IllegalStateException("Sign is removed");
            } else {
                throw new IllegalStateException("Sign at world=" + block.getWorld().getName() +
                        " x=" + block.getX() + " y=" + block.getY() + " z=" + block.getZ() +
                        " is removed");
            }
        }
    }

    protected boolean isTileRemoved(TileEntitySignHandle tileEntity) {
        return tileEntity.isRemoved();
    }

    private boolean detectChangedLines(TileEntitySignHandle tileEntity) {
        Object[] oldRawFrontLines = this.lastRawFrontLines;
        Object[] newRawFrontLines = tileEntity.getRawFrontLines();

        Object[] oldRawBackLines = this.lastRawBackLines;
        Object[] newRawBackLines = tileEntity.getRawBackLines();

        if (oldRawFrontLines.length != newRawFrontLines.length ||
                (CommonCapabilities.HAS_SIGN_BACK_TEXT && oldRawBackLines.length != newRawBackLines.length)
        ) {
            // Never happens, really
            this.lastRawFrontLines = newRawFrontLines.clone();
            if (CommonCapabilities.HAS_SIGN_BACK_TEXT) {
                this.lastRawBackLines = newRawBackLines.clone();
            }
        } else if (!copyLinesCheckChanges(oldRawFrontLines, newRawFrontLines)) {
            if (!CommonCapabilities.HAS_SIGN_BACK_TEXT || !copyLinesCheckChanges(oldRawBackLines, newRawBackLines)) {
                // No changes.
                return false;
            }
        } else if (CommonCapabilities.HAS_SIGN_BACK_TEXT) {
            // Front changed, take over changes of back blindly as we haven't done the copy for thise
            System.arraycopy(oldRawBackLines, 0, newRawBackLines, 0, newRawBackLines.length);
        }

        // Reset cached 'stringified' lines
        this.lastMessageFrontLines = null;
        this.lastMessageBackLines = null;

        // Detected a change. Re-create the Sign state with the updated lines,
        // and return true to indicate the change.
        this.state = this.tileEntity.toBukkit();
        return true;
    }

    private static boolean copyLinesCheckChanges(Object[] oldRawLines, Object[] newRawLines) {
        int numLines = newRawLines.length;
        int line = 0;
        while (line < numLines) {
            Object newLine = newRawLines[line];
            if (oldRawLines[line] != newLine) {
                oldRawLines[line] = newLine;

                // Copy remaining lines over, too
                while (++line < numLines) {
                    oldRawLines[line] = newRawLines[line];
                }

                return true;
            }

            line++;
        }
        return false;
    }

    private boolean tryLoadFromWorld() {
        Block block = this.block;
        Object rawTileEntity;
        if (MaterialUtil.ISSIGN.get(block) && // Initiates sync chunk load if needed
            (rawTileEntity = CraftBlockHandle.getBlockTileEntity(block)) != null &&
            TileEntitySignHandle.T.isAssignableFrom(rawTileEntity)
        ) {
            TileEntitySignHandle tileEntity = TileEntitySignHandle.createHandle(rawTileEntity);
            this.state = tileEntity.toBukkit();
            this.loadTileEntity(tileEntity);
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("SignChangeTracker{block=");
        if (block == null) {
            str.append("null");
        } else {
            str.append("{world=").append(block.getWorld().getName())
                    .append(", x=").append(block.getX())
                    .append(", y=").append(block.getY())
                    .append(", z=").append(block.getZ())
                    .append('}');
        }

        str.append(", lines=");

        Sign sign = this.getSign();
        if (sign == null) {
            str.append("<REMOVED>");
        } else {
            str.append('[');
            for (int i = 0; i < 4; i++) {
                if (i > 0) {
                    str.append(", ");
                }
                str.append(sign.getLine(i));
            }
            str.append(']');
        }
        str.append('}');
        return str.toString();
    }

    @Override
    public SignChangeTracker clone() {
        SignChangeTracker clone = new SignChangeTracker(this.block);
        clone.state = this.state;
        clone.blockData = this.blockData;
        clone.tileEntity = this.tileEntity;
        clone.lastRawFrontLines = this.lastRawFrontLines;
        clone.lastRawBackLines = this.lastRawBackLines;
        clone.lastMessageFrontLines = this.lastMessageFrontLines;
        clone.lastMessageBackLines = this.lastMessageBackLines;
        return clone;
    }

    /**
     * Tracks the changes done to a Sign
     *
     * @param sign The sign to track
     * @return Sign change tracker
     */
    public static SignChangeTracker track(Sign sign) {
        if (sign == null) {
            throw new IllegalArgumentException("Sign is null");
        } else {
            SignChangeTracker tracker = constructor.apply(sign.getBlock());
            tracker.initState(sign);
            return tracker;
        }
    }

    /**
     * Tracks the changes done to a Sign
     *
     * @param signBlock The block of the sign to track
     * @return Sign change tracker
     */
    public static SignChangeTracker track(Block signBlock) {
        SignChangeTracker tracker = constructor.apply(signBlock);
        Sign state = BlockUtil.getSign(signBlock);
        if (state != null) {
            tracker.initState(state);
        }
        return tracker;
    }

    /**
     * Some old 1.12.2 versions of the server had a bug that TileEntity isRemoved() didn't work at all.
     * This tracker checks the index at which a TileEntity is stored in a List, and checks that it
     * has been removed from it that way.
     */
    private static class SignChangeTrackerMohistLegacy extends SignChangeTracker {
        private final List<Object> worldTileEntities;
        private int lastIndex = -1;
 
        protected SignChangeTrackerMohistLegacy(Block block, List<Object> worldTileEntities) {
            super(block);
            this.worldTileEntities = worldTileEntities;
        }

        @Override
        protected boolean isTileRemoved(TileEntitySignHandle tileEntity) {
            List<Object> list = this.worldTileEntities;
            Object rawTileEntity = tileEntity.getRaw();
            if (lastIndex >= 0 && lastIndex < list.size() && list.get(lastIndex) == rawTileEntity) {
                return false;
            }
            lastIndex = list.indexOf(rawTileEntity);
            return lastIndex == -1;
        }
    }
}
