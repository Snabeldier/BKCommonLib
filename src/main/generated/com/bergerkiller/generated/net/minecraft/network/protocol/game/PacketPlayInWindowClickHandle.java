package com.bergerkiller.generated.net.minecraft.network.protocol.game;

import com.bergerkiller.mountiplex.reflection.declarations.Template;
import com.bergerkiller.bukkit.common.wrappers.InventoryClickType;
import com.bergerkiller.generated.net.minecraft.network.protocol.PacketHandle;
import org.bukkit.inventory.ItemStack;

/**
 * Instance wrapper handle for type <b>net.minecraft.network.protocol.game.PacketPlayInWindowClick</b>.
 * To access members without creating a handle type, use the static {@link #T} member.
 * New handles can be created from raw instances using {@link #createHandle(Object)}.
 */
@Template.InstanceType("net.minecraft.network.protocol.game.PacketPlayInWindowClick")
public abstract class PacketPlayInWindowClickHandle extends PacketHandle {
    /** @see PacketPlayInWindowClickClass */
    public static final PacketPlayInWindowClickClass T = Template.Class.create(PacketPlayInWindowClickClass.class, com.bergerkiller.bukkit.common.Common.TEMPLATE_RESOLVER);
    /* ============================================================================== */

    public static PacketPlayInWindowClickHandle createHandle(Object handleInstance) {
        return T.createHandle(handleInstance);
    }

    /* ============================================================================== */

    public abstract int getWindowId();
    public abstract void setWindowId(int value);
    public abstract int getSlot();
    public abstract void setSlot(int value);
    public abstract int getButton();
    public abstract void setButton(int value);
    public abstract ItemStack getItem();
    public abstract void setItem(ItemStack value);
    public abstract InventoryClickType getMode();
    public abstract void setMode(InventoryClickType value);
    /**
     * Stores class members for <b>net.minecraft.network.protocol.game.PacketPlayInWindowClick</b>.
     * Methods, fields, and constructors can be used without using Handle Objects.
     */
    public static final class PacketPlayInWindowClickClass extends Template.Class<PacketPlayInWindowClickHandle> {
        public final Template.Field.Integer windowId = new Template.Field.Integer();
        public final Template.Field.Integer slot = new Template.Field.Integer();
        public final Template.Field.Integer button = new Template.Field.Integer();
        public final Template.Field.Converted<ItemStack> item = new Template.Field.Converted<ItemStack>();
        public final Template.Field.Converted<InventoryClickType> mode = new Template.Field.Converted<InventoryClickType>();

    }

}

