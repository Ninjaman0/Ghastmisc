package com.ninja.ghastmisc.listeners;

import com.ninja.ghastmisc.GhastMiscPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandInterceptListener implements Listener {

    private final GhastMiscPlugin plugin;

    public CommandInterceptListener(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().substring(1); // Remove the '/' prefix

        if (plugin.getCommandManager().handleCommand(command, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();

        if (plugin.getCommandManager().handleCommand(command, event.getSender())) {
            event.setCancelled(true);
        }
    }
}