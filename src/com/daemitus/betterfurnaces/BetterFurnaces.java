package com.daemitus.betterfurnaces;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterFurnaces extends JavaPlugin {

    private final String version = "v1.0";

    public void onEnable() {
        PluginManager pm = this.getServer().getPluginManager();

        PlayerListener playerListener = new PlayerListener(this);

        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);

        System.out.println("[BetterFurnaces] " + version + " Enabled!");
    }

    public void onDisable() {
    }
}
