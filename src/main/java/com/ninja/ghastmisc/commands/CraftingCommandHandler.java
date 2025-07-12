package com.ninja.ghastmisc.commands;

import com.ninja.ghastmisc.GhastMiscPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CraftingCommandHandler {

    private final GhastMiscPlugin plugin;

    public CraftingCommandHandler(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6Crafting Commands:");
            sender.sendMessage("§7/gm craft gui §8- §fOpen crafting GUI");
            sender.sendMessage("§7/gm craft list <recipes/ingredients> §8- §fList recipes or ingredients");
            sender.sendMessage("§7/gm craft register <item_id> §8- §fRegister item in hand");
            sender.sendMessage("§7/gm craft give <item_id> [player] [amount] §8- §fGive custom item");
            sender.sendMessage("§7/gm craft take <item_id> [player] [amount] §8- §fTake custom item");
            sender.sendMessage("§7/gm craft view <id> §8- §fView recipe");
            sender.sendMessage("§7/gm craft editor <id> §8- §fEdit recipe");
            return true;
        }

        String subcommand = args[1].toLowerCase();

        switch (subcommand) {
            case "gui":
                return handleGUI(sender);
            case "list":
                return handleList(sender, args);
            case "register":
                return handleRegister(sender, args);
            case "give":
                return handleGive(sender, args);
            case "take":
                return handleTake(sender, args);
            case "view":
                return handleView(sender, args);
            case "editor":
                return handleEditor(sender, args);
            default:
                sender.sendMessage("§cUnknown crafting subcommand: " + subcommand);
                return true;
        }
    }

    private boolean handleGUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.craft")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        Player player = (Player) sender;
        plugin.getCraftingManager().openCraftingGUI(player);
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm craft list <recipes/ingredients>");
            return true;
        }

        String type = args[2].toLowerCase();
        if (type.equals("recipes")) {
            plugin.getCraftingManager().listRecipes(sender);
        } else if (type.equals("ingredients")) {
            plugin.getCraftingManager().listIngredients(sender);
        } else {
            sender.sendMessage("§cInvalid type! Use 'recipes' or 'ingredients'");
        }

        return true;
    }

    private boolean handleRegister(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm craft register <item_id>");
            return true;
        }

        Player player = (Player) sender;
        String itemId = args[2];
        plugin.getCraftingManager().registerItem(player, itemId);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm craft give <item_id> [player] [amount]");
            return true;
        }

        String itemId = args[2];
        Player targetPlayer = null;
        int amount = 1;

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

        if (args.length > 4) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(sender, "general.invalid-amount", "amount", args[4]);
                return true;
            }
        }

        plugin.getCraftingManager().giveItem(sender, targetPlayer, itemId, amount);
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm craft take <item_id> [player] [amount]");
            return true;
        }

        String itemId = args[2];
        Player targetPlayer = null;
        int amount = 1;

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

        if (args.length > 4) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(sender, "general.invalid-amount", "amount", args[4]);
                return true;
            }
        }

        plugin.getCraftingManager().takeItem(sender, targetPlayer, itemId, amount);
        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm craft view <id>");
            return true;
        }

        Player player = (Player) sender;
        String itemId = args[2];
        plugin.getCraftingManager().viewRecipe(player, itemId);
        return true;
    }

    private boolean handleEditor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gm craft editor <id>");
            return true;
        }

        Player player = (Player) sender;
        String itemId = args[2];
        plugin.getCraftingManager().openRecipeEditor(player, itemId);
        return true;
    }

    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("gui", "list", "register", "give", "take", "view", "editor");
        }

        if (args.length == 3) {
            String subcommand = args[1].toLowerCase();
            switch (subcommand) {
                case "list":
                    return Arrays.asList("recipes", "ingredients");
                case "give":
                case "take":
                case "view":
                case "editor":
                    return new ArrayList<>(plugin.getCraftingManager().getRecipeIds());
            }
        }

        return new ArrayList<>();
    }
}