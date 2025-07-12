package com.ninja.ghastmisc.commands;

import com.ninja.ghastmisc.GhastMiscPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandReplacementHandler {

    private final GhastMiscPlugin plugin;

    public CommandReplacementHandler(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§6Command Replacement Commands:");
            sender.sendMessage("§7/gm commands list §8- §fList all command IDs");
            sender.sendMessage("§7/gm command debug <id> §8- §fDebug command by ID");
            sender.sendMessage("§7/gm command test <id> [player] §8- §fTest command by ID");
            return true;
        }

        String subcommand = args[1].toLowerCase();

        switch (subcommand) {
            case "list":
                return handleList(sender);
            case "debug":
                return handleDebug(sender, args);
            case "test":
                return handleTest(sender, args);
            default:
                sender.sendMessage("§cUnknown command subcommand: " + subcommand);
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        plugin.getCommandManager().listCommands(sender);
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm command debug <id>");
            return true;
        }

        String commandId = args[2];
        plugin.getCommandManager().debugCommand(sender, commandId);
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm command test <id> [player]");
            return true;
        }

        String commandId = args[2];
        Player targetPlayer = null;

        if (args.length > 3) {
            targetPlayer = Bukkit.getPlayer(args[3]);
            if (targetPlayer == null) {
                plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", args[3]);
                return true;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
        } else {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        plugin.getCommandManager().testCommand(sender, commandId, targetPlayer);
        return true;
    }

    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("list", "debug", "test");
        }

        if (args.length == 3) {
            String subcommand = args[1].toLowerCase();
            if (subcommand.equals("debug") || subcommand.equals("test")) {
                return new ArrayList<>(plugin.getCommandManager().getCommandIds());
            }
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("test")) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }

        return new ArrayList<>();
    }
}