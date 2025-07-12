package com.ninja.ghastmisc.commands;

import com.ninja.ghastmisc.GhastMiscPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GhastMiscCommand implements CommandExecutor, TabCompleter {

    private final GhastMiscPlugin plugin;
    private final CraftingCommandHandler craftingHandler;
    private final VoucherCommandHandler voucherHandler;
    private final CommandReplacementHandler commandHandler;

    public GhastMiscCommand(GhastMiscPlugin plugin) {
        this.plugin = plugin;
        this.craftingHandler = new CraftingCommandHandler(plugin);
        this.voucherHandler = new VoucherCommandHandler(plugin);
        this.commandHandler = new CommandReplacementHandler(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "craft":
                return craftingHandler.handleCommand(sender, args);
            case "voucher":
                return voucherHandler.handleCommand(sender, args);
            case "command":
            case "commands":
                return commandHandler.handleCommand(sender, args);
            case "autocraft":
                return handleAutocraft(sender);
            case "onecraft":
                return handleOneCraft(sender);
            case "compactor":
                return handleCompactor(sender);
            case "reload":
                return handleReload(sender);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage("§cUnknown subcommand: " + subcommand);
                sender.sendMessage("§7Use §f/gm help §7for available commands");
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§l=== GhastMisc Help ===");
        sender.sendMessage("§6Version: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("");
        sender.sendMessage("§6Crafting System:");
        sender.sendMessage("§7/gm craft gui §8- §fOpen custom crafting table");
        sender.sendMessage("§7/gm craft list <recipes/ingredients> §8- §fList items");
        sender.sendMessage("§7/gm craft register <id> §8- §fRegister item in hand");
        sender.sendMessage("§7/gm craft give <id> [player] [amount] §8- §fGive custom item");
        sender.sendMessage("§7/gm craft take <id> [player] [amount] §8- §fTake custom item");
        sender.sendMessage("§7/gm craft view <id> §8- §fView recipe");
        sender.sendMessage("§7/gm craft editor <id> §8- §fEdit recipe");
        sender.sendMessage("§7/gm autocraft §8- §fToggle auto-crafting");
        sender.sendMessage("§7/gm onecraft §8- §fCraft item in hand once");
        sender.sendMessage("§7/gm compactor §8- §fOpen compactor GUI");
        sender.sendMessage("");
        sender.sendMessage("§6Voucher System:");
        sender.sendMessage("§7/gm voucher <type> <time> <multiplier> <player> §8- §fGive voucher");
        sender.sendMessage("");
        sender.sendMessage("§6Command System:");
        sender.sendMessage("§7/gm commands list §8- §fList command replacements");
        sender.sendMessage("§7/gm command debug <id> §8- §fDebug command");
        sender.sendMessage("§7/gm command test <id> [player] §8- §fTest command");
        sender.sendMessage("");
        sender.sendMessage("§6General:");
        sender.sendMessage("§7/gm reload §8- §fReload all configurations");
        sender.sendMessage("§7/gm help §8- §fShow this help message");
    }

    private boolean handleAutocraft(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.autocraft")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        Player player = (Player) sender;
        boolean enabled = plugin.getAutoCraftManager().toggleAutocraft(player);

        if (enabled) {
            plugin.getMessageManager().sendMessage(player, "crafting.autocraft-enabled");
        } else {
            plugin.getMessageManager().sendMessage(player, "crafting.autocraft-disabled");
        }

        return true;
    }

    private boolean handleOneCraft(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.onecraft")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        Player player = (Player) sender;
        boolean success = plugin.getAutoCraftManager().performOneCraft(player);

        if (success) {
            plugin.getMessageManager().sendMessage(player, "crafting.onecraft-success");
        } else {
            plugin.getMessageManager().sendMessage(player, "crafting.onecraft-failed");
        }

        return true;
    }

    private boolean handleCompactor(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.compactor")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        Player player = (Player) sender;
        plugin.getCompactorManager().openCompactorGUI(player);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        sender.sendMessage("§6§l[GhastMisc] §7Reloading configurations...");

        try {
            plugin.reload();
            plugin.getMessageManager().sendMessage(sender, "general.reload-success");

            // Log reload action
            plugin.getLogger().info("Configurations reloaded by " + sender.getName());

        } catch (Exception e) {
            plugin.getMessageManager().sendMessage(sender, "general.reload-error", "error", e.getMessage());
            plugin.getLogger().severe("Error during config reload: " + e.getMessage());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("craft", "voucher", "command", "commands", "autocraft", "onecraft", "compactor", "reload", "help");
            String input = args[0].toLowerCase();

            for (String subcommand : subcommands) {
                if (subcommand.startsWith(input)) {
                    completions.add(subcommand);
                }
            }

            return completions;
        }

        if (args.length > 1) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "craft":
                    return craftingHandler.getTabComplete(sender, args);
                case "voucher":
                    return voucherHandler.getTabComplete(sender, args);
                case "command":
                case "commands":
                    return commandHandler.getTabComplete(sender, args);
            }
        }

        return completions;
    }
}