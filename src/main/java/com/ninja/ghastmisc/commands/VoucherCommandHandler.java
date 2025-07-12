package com.ninja.ghastmisc.commands;

import com.ninja.ghastmisc.GhastMiscPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoucherCommandHandler {

    private final GhastMiscPlugin plugin;

    public VoucherCommandHandler(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghastmisc.admin")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 5) {
            sender.sendMessage("§cUsage: /gm voucher <section> <time> <multiplier> <player>");
            sender.sendMessage("§cSections: levels, essence, money");
            return true;
        }

        String section = args[1].toLowerCase();
        String timeStr = args[2];
        String multiplierStr = args[3];
        String playerName = args[4];

        if (!Arrays.asList("levels", "essence", "money").contains(section)) {
            sender.sendMessage("§cInvalid section! Use: levels, essence, money");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", playerName);
            return true;
        }

        try {
            int time = Integer.parseInt(timeStr);
            double multiplier = Double.parseDouble(multiplierStr);

            plugin.getVoucherManager().giveVoucher(sender, targetPlayer, section, time, multiplier);
            return true;

        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "general.invalid-amount", "amount", timeStr + " or " + multiplierStr);
            return true;
        }
    }

    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("levels", "essence", "money");
        }

        if (args.length == 5) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }

        return new ArrayList<>();
    }
}