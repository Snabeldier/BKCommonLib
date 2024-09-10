package com.bergerkiller.bukkit.common.controller;

import com.bergerkiller.bukkit.common.internal.logic.PlayerFileDataHandler;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;

import org.bukkit.entity.Player;

/**
 * A controller for dealing with player data loading and saving. To hook it up
 * to the server, call {@link #assign()}.
 */
public class PlayerDataController {
    private PlayerFileDataHandler.Hook hook = null;

    /**
     * Called when the entity data for a player has to be loaded. By
     * default, this method redirects to the underlying implementation.
     *
     * @param player to load
     * @return the loaded data, or <i>null</i> if no data is available
     */
    public CommonTagCompound onLoad(Player player) {
        return hook.base_load(player);
    }

    /**
     * Called when the entity data for a player has to be loaded, for a player
     * that is not currently online. This is called on Minecraft 1.20.5 and later,
     * and if {@link #onLoad(Player)} isn't overrided, is called also for online
     * players.
     *
     * @param playerName Name of the player
     * @param playerUUID UUID String of the player. Could be an invalid UUID!
     * @return the loaded data, or <i>null</i> if no data is available
     */
    public CommonTagCompound onLoadOffline(String playerName, String playerUUID) {
        return hook.base_load_offline(playerName, playerUUID);
    }

    /**
     * Called when the entity data of a player has to be saved. By
     * default, this method redirects to the underlying implementation.
     *
     * @param player to save
     */
    public void onSave(Player player) {
        hook.base_save(player);
    }

    /**
     * Assigns this PlayerDataController to the server
     */
    public void assign() {
        hook = PlayerFileDataHandler.INSTANCE.hook(this);
    }

    /**
     * Detaches this PlayerDataController from the server; it will no longer be used
     */
    public void detach() {
        if (this.hook != null) {
            PlayerFileDataHandler.INSTANCE.unhook(this.hook, this);
            this.hook = null;
        }
    }

    /**
     * Obtains the Player Data Controller currently assigned to the server.
     * If no custom controller is assigned, a default instance is returned that allows
     * interaction with the default controller.
     *
     * @return the currently assigned Player Data Controller
     */
    public static PlayerDataController get() {
        PlayerDataController controller = PlayerFileDataHandler.INSTANCE.get();
        if (controller == null) {
            controller = new PlayerDataController();
            controller.hook = PlayerFileDataHandler.INSTANCE.mock(controller);
        }
        return controller;
    }
}
