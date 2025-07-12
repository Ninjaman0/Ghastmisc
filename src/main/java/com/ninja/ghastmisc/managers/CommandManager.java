package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public class CommandManager {

    private final GhastMiscPlugin plugin;
    private final Map<String, CommandReplacement> commandReplacements = new HashMap<>();
    private final Map<String, DynamicCommand> dynamicCommands = new HashMap<>();

    public CommandManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;
        loadCommandData();
    }

    public void loadCommandData() {
        commandReplacements.clear();

        ConfigurationSection commands = plugin.getConfigManager().getCommandsConfig().getConfigurationSection("commands");
        if (commands != null) {
            for (String commandId : commands.getKeys(false)) {
                ConfigurationSection commandSection = commands.getConfigurationSection(commandId);
                if (commandSection != null) {
                    CommandReplacement replacement = loadCommandReplacement(commandId, commandSection);
                    if (replacement != null) {
                        commandReplacements.put(commandId, replacement);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + commandReplacements.size() + " command replacements");
    }

    private CommandReplacement loadCommandReplacement(String id, ConfigurationSection config) {
        try {
            String command = config.getString("command", "");
            boolean opOnly = config.getBoolean("op-only", false);
            List<String> result = config.getStringList("result");
            String permission = config.getString("permission");
            List<String> tabComplete = config.getStringList("tab-complete");

            if (command.isEmpty() || result.isEmpty()) {
                plugin.getLogger().warning("Invalid command replacement: " + id);
                return null;
            }

            return new CommandReplacement(id, command, opOnly, result, permission, tabComplete);

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading command replacement " + id + ": " + e.getMessage());
            return null;
        }
    }

    public void registerDynamicCommands() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // Unregister old dynamic commands
            for (DynamicCommand dynamicCommand : dynamicCommands.values()) {
                Command existingCommand = commandMap.getCommand(dynamicCommand.getName());
                if (existingCommand != null) {
                    existingCommand.unregister(commandMap);
                }
            }
            dynamicCommands.clear();

            // Register new dynamic commands
            for (CommandReplacement replacement : commandReplacements.values()) {
                String commandName = replacement.getCommand().trim().split(" ")[0];
                if (!commandName.isEmpty()) {
                    DynamicCommand dynamicCommand = new DynamicCommand(commandName, replacement);
                    commandMap.register(plugin.getName(), dynamicCommand);
                    dynamicCommands.put(commandName, dynamicCommand);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error registering dynamic commands: " + e.getMessage());
        }
    }

    public boolean handleCommand(String commandLine, CommandSender sender) {
        // Normalize command line
        commandLine = commandLine.trim();

        for (CommandReplacement replacement : commandReplacements.values()) {
            String targetCommand = replacement.getCommand().trim();

            // Check if the command matches
            if (commandLine.equals(targetCommand) ||
                    (commandLine.startsWith(targetCommand + " ") && targetCommand.endsWith(" "))) {

                // Check permissions
                if (replacement.getPermission() != null && !sender.hasPermission(replacement.getPermission())) {
                    continue;
                }

                // Check if op-only and sender is not op
                if (replacement.isOpOnly() && !sender.isOp()) {
                    continue;
                }

                // Extract arguments from command
                String args = "";
                if (commandLine.length() > targetCommand.length()) {
                    args = commandLine.substring(targetCommand.length()).trim();
                }

                // Extract player name if present
                String playerName = "";
                if (!args.isEmpty()) {
                    String[] argParts = args.split(" ");
                    playerName = argParts[0];
                }

                // Execute replacement commands
                for (String resultCommand : replacement.getResult()) {
                    String finalCommand = resultCommand
                            .replace("%player_name%", playerName)
                            .replace("%args%", args);

                    // Parse PlaceholderAPI placeholders if sender is a player
                    if (sender instanceof Player && plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        finalCommand = PlaceholderAPI.setPlaceholders((Player) sender, finalCommand);
                    }

                    String finalCommand1 = finalCommand;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                finalCommand1);
                    });
                }

 return true;
            }
        }

        return false;
    }

    public void listCommands(CommandSender sender) {
        sender.sendMessage("§6Command Replacements:");
        for (String commandId : commandReplacements.keySet()) {
            CommandReplacement replacement = commandReplacements.get(commandId);
            sender.sendMessage("§7- §f" + commandId + " §8(§f" + replacement.getCommand() + "§8)");
        }
    }

    public void debugCommand(CommandSender sender, String commandId) {
        if (!commandReplacements.containsKey(commandId)) {
            plugin.getMessageManager().sendMessage(sender, "commands.not-found", "id", commandId);
            return;
        }

        CommandReplacement replacement = commandReplacements.get(commandId);
        sender.sendMessage("§6Command Debug: " + commandId);
        sender.sendMessage("§7Command: §f" + replacement.getCommand());
        sender.sendMessage("§7OP Only: §f" + replacement.isOpOnly());
        sender.sendMessage("§7Permission: §f" + (replacement.getPermission() != null ? replacement.getPermission() : "None"));
        sender.sendMessage("§7Result Commands:");
        for (String resultCommand : replacement.getResult()) {
            sender.sendMessage("§8  - §f" + resultCommand);
        }
    }

    public void testCommand(CommandSender sender, String commandId, Player targetPlayer) {
        if (!commandReplacements.containsKey(commandId)) {
            plugin.getMessageManager().sendMessage(sender, "commands.not-found", "id", commandId);
            return;
        }

        CommandReplacement replacement = commandReplacements.get(commandId);
        plugin.getMessageManager().sendMessage(sender, "commands.testing", "id", commandId, "player", targetPlayer.getName());

        // Execute replacement commands
        for (String resultCommand : replacement.getResult()) {
            String finalCommand = resultCommand.replace("%player_name%", targetPlayer.getName());

            // Parse PlaceholderAPI placeholders
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                finalCommand = PlaceholderAPI.setPlaceholders(targetPlayer, finalCommand);
            }

            plugin.getMessageManager().sendMessage(sender, "commands.executing", "command", finalCommand);

            String finalCommand1 = finalCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand1);
            });
        }
    }

    public Set<String> getCommandIds() {
        return commandReplacements.keySet();
    }

    // Inner classes
    public static class CommandReplacement {
        private final String id;
        private final String command;
        private final boolean opOnly;
        private final List<String> result;
        private final String permission;
        private final List<String> tabComplete;

        public CommandReplacement(String id, String command, boolean opOnly, List<String> result, String permission, List<String> tabComplete) {
            this.id = id;
            this.command = command;
            this.opOnly = opOnly;
            this.result = result;
            this.permission = permission;
            this.tabComplete = tabComplete != null ? tabComplete : new ArrayList<>();
        }

        // Getters
        public String getId() { return id; }
        public String getCommand() { return command; }
        public boolean isOpOnly() { return opOnly; }
        public List<String> getResult() { return result; }
        public String getPermission() { return permission; }
        public List<String> getTabComplete() { return tabComplete; }
    }

    public class DynamicCommand extends BukkitCommand {
        private final CommandReplacement replacement;

        public DynamicCommand(String name, CommandReplacement replacement) {
            super(name);
            this.replacement = replacement;
            setDescription("Dynamic command from GhastMisc");
            if (replacement.getPermission() != null) {
                setPermission(replacement.getPermission());
            }
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            // Check permissions
            if (replacement.getPermission() != null && !sender.hasPermission(replacement.getPermission())) {
                plugin.getMessageManager().sendMessage(sender, "general.no-permission");
                return true;
            }

            // Check if op-only
            if (replacement.isOpOnly() && !sender.isOp()) {
                plugin.getMessageManager().sendMessage(sender, "general.no-permission");
                return true;
            }

            // Build full command
            StringBuilder fullCommand = new StringBuilder(commandLabel);
            for (String arg : args) {
                fullCommand.append(" ").append(arg);
            }

            // Handle the command
            plugin.getCommandManager().handleCommand(fullCommand.toString(), sender);
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            // Check permissions for tab completion
            if (replacement.getPermission() != null && !sender.hasPermission(replacement.getPermission())) {
                return new ArrayList<>();
            }

            if (replacement.isOpOnly() && !sender.isOp()) {
                return new ArrayList<>();
            }

            // Return custom tab completions if available
            if (!replacement.getTabComplete().isEmpty()) {
                return replacement.getTabComplete();
            }

            // Default tab completion (online players)
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String input = args[0].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
            return completions;
        }
    }
}