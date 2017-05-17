package com.bergerkiller.bukkit.common.utils;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.internal.CommonNMS;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.generated.net.minecraft.server.EntityHandle;
import com.bergerkiller.generated.net.minecraft.server.EntityLivingHandle;
import com.bergerkiller.generated.net.minecraft.server.GenericAttributesHandle;
import com.bergerkiller.reflection.net.minecraft.server.NMSEntityHanging;

import net.minecraft.server.v1_11_R1.Entity;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class EntityUtil extends EntityPropertyUtil {

    /**
     * Finds an Entity that has the given entity UUID and is of the type
     * specified from a world
     *
     * @param world to find the Entity
     * @param uid of the Entity to find
     * @param type Class of the Entity to find
     * @return the found Entity, or null if not found
     */
    public static <T extends org.bukkit.entity.Entity> T getEntity(org.bukkit.World world, UUID uid, Class<T> type) {
        return CommonUtil.tryCast(getEntity(world, uid), type);
    }

    /**
     * Finds an Entity that has the given entity UUID from a world
     *
     * @param world to find the Entity
     * @param uid of the Entity to find
     * @return the found Entity, or null if not found
     */
    public static org.bukkit.entity.Entity getEntity(org.bukkit.World world, UUID uid) {
        for (org.bukkit.entity.Entity entity : WorldUtil.getEntities(world)) {
            if (entity.getUniqueId().equals(uid)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Adds a single entity to the server
     *
     * @param entity to add
     */
    public static void addEntity(org.bukkit.entity.Entity entity) {
        Entity nmsentity = CommonNMS.getNative(entity);
        nmsentity.world.getChunkAt(MathUtil.toChunk(nmsentity.locX), MathUtil.toChunk(nmsentity.locZ));
        nmsentity.dead = false;
        // Remove an entity tracker for this entity if it was present
        WorldUtil.getTracker(entity.getWorld()).stopTracking(entity);
        // Add the entity to the world
        nmsentity.world.addEntity(nmsentity);
    }

    /**
     * Changes the speed of a living entity
     *
     * @param entity Entity
     * @param speed New entity speed
     */
    public static void setSpeed(LivingEntity entity, double speed) {
        EntityLivingHandle nmsEntity = CommonNMS.getHandle(entity);
        nmsEntity.getAttributeInstance(GenericAttributesHandle.MOVEMENT_SPEED).setValue(speed);
    }

    /**
     * Gets the speed of a living entity
     *
     * @param entity to check speed
     * @return entity speed
     */
    public static double getSpeed(LivingEntity entity) {
        EntityLivingHandle nmsEntity = CommonNMS.getHandle(entity);
        return nmsEntity.getAttributeInstance(GenericAttributesHandle.MOVEMENT_SPEED).getValue();
    }

    /**
     * Checks whether a given Entity should be ignored when working with it<br>
     * This could be because another plugin is operating on it, or for Virtual
     * items
     *
     * @param entity to check
     * @return True if the entity should be ignored, False if not
     */
    public static boolean isIgnored(org.bukkit.entity.Entity entity) {
        return CommonPlugin.getInstance().getEntityBlacklist().isFiltered(entity);
    }

    /*
     * Is near something?
     */
    public static boolean isNearChunk(org.bukkit.entity.Entity entity, final int cx, final int cz, final int chunkview) {
        final int x = MathUtil.toChunk(getLocX(entity)) - cx;
        final int z = MathUtil.toChunk(getLocZ(entity)) - cz;
        return Math.abs(x) <= chunkview && Math.abs(z) <= chunkview;
    }

    public static boolean isNearBlock(org.bukkit.entity.Entity entity, final int bx, final int bz, final int blockview) {
        final int x = MathUtil.floor(getLocX(entity) - bx);
        final int z = MathUtil.floor(getLocZ(entity) - bz);
        return Math.abs(x) <= blockview && Math.abs(z) <= blockview;
    }

    /**
     * Performs entity on entity collision logic for an entity. This will
     * perform the push logic caused by collision.
     *
     * @param entity to work on
     * @param with the entity to collide
     */
    public static void doCollision(org.bukkit.entity.Entity entity, org.bukkit.entity.Entity with) {
        CommonNMS.getNative(entity).collide(CommonNMS.getNative(with));
    }

    /**
     * Teleports an entity in the next tick
     *
     * @param entity to teleport
     * @param to location to teleport to
     */
    public static void teleportNextTick(final org.bukkit.entity.Entity entity, final Location to) {
        CommonUtil.nextTick(new Runnable() {
            public void run() {
                teleport(entity, to);
            }
        });
    }

    /**
     * Teleports an entity
     *
     * @param entity to teleport
     * @param to location to teleport to
     */
    public static boolean teleport(final org.bukkit.entity.Entity entity, final Location to) {
        return CommonEntity.get(entity).teleport(to);
    }

    /**
     * Gets a new Entity Id that will be unique during the current server session run
     * 
     * @return entity Id
     */
    public static int getUniqueEntityId() {
        int id = EntityHandle.entityCount();
        EntityHandle.entityCount_set(id + 1);
        return id;
    }

    /**
     * Gets the Block a hanging entity is attached to
     * 
     * @param entityHanging
     * @return block
     */
    public static Block getHangingBlock(Hanging entityHanging) {
        IntVector3 pos = NMSEntityHanging.blockPosition.get(Conversion.toEntityHandle.convert(entityHanging));
        return pos.toBlock(entityHanging.getWorld());
    }
}
