package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class MessageManager {
    private final GhastMiscPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;
        setupMessagesFile();
        loadMessages();
    }

    private void setupMessagesFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        saveDefaultMessages();
    }

    private void saveDefaultMessages() {
        if (!messagesFile.exists()) {
            try {
                InputStream resource = plugin.getResource("messages.yml");
                if (resource != null) {
                    Files.copy(resource, messagesFile.toPath());
                } else {
                    createDefaultMessages();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create messages.yml: " + e.getMessage());
                createDefaultMessages();
            }
        }
    }

    private void createDefaultMessages() {
        try {
            if (messagesFile.createNewFile()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);

                // General Messages
                config.set("general.no-permission", "&cYou don't have permission to use this command!");
                config.set("general.player-only", "&cThis command can only be used by players!");
                config.set("general.player-not-found", "&cPlayer not found: {player}");
                config.set("general.invalid-amount", "&cInvalid amount: {amount}");
                config.set("general.reload-success", "&aConfiguration reloaded successfully!");
                config.set("general.reload-error", "&cError reloading configuration: {error}");

                // Crafting Messages
                config.set("crafting.gui-opened", "&aCustom crafting table opened!");
                config.set("crafting.item-registered", "&aItem registered as ingredient: {id}");
                config.set("crafting.item-given", "&aGave {amount} {item} to {player}");
                config.set("crafting.item-taken", "&aRemoved {amount} {item} from {player}");
                config.set("crafting.item-not-found", "&cItem not found: {id}");
                config.set("crafting.recipe-not-found", "&cRecipe not found: {id}");
                config.set("crafting.no-permission-craft", "&cYou don't have permission to craft this item!");
                config.set("crafting.inventory-full", "&cYour inventory is full! Cannot craft items.");
                config.set("crafting.compactor-opened", "&aCompactor opened! Click items in your inventory to add them.");
                config.set("crafting.autocraft-enabled", "&a&l[AutoCraft] &aAuto-crafting enabled!");
                config.set("crafting.autocraft-disabled", "&c&l[AutoCraft] &cAuto-crafting disabled!");
                config.set("crafting.onecraft-success", "&a&l[OneCraft] &aSuccessfully crafted item!");
                config.set("crafting.onecraft-failed", "&c&l[OneCraft] &cCould not craft item! Check ingredients and inventory space.");
                config.set("crafting.recipe-saved", "&aRecipe saved successfully!");
                config.set("crafting.recipe-cancelled", "&cRecipe editing cancelled.");
                config.set("crafting.compactor-item-added", "&aItem added to compactor!");
                config.set("crafting.compactor-item-removed", "&cItem removed from compactor!");
                config.set("crafting.compactor-invalid-item", "&cThis item cannot be compacted!");
                config.set("crafting.compactor-no-recipe", "&cNo recipe found for this item!");
                config.set("crafting.compactor-full", "&cCompactor is full! Remove some items first.");

                // Voucher Messages
                config.set("voucher.received", "&aYou received a {type} voucher!");
                config.set("voucher.given", "&aGave {type} voucher to {player} (Time: {time}min, Multiplier: {multiplier}x)");
                config.set("voucher.used", "&aVoucher used successfully!");
                config.set("voucher.cancelled", "&cVoucher use cancelled.");
                config.set("voucher.invalid", "&cInvalid voucher!");
                config.set("voucher.invalid-data", "&cInvalid voucher data!");
                config.set("voucher.type-not-found", "&cVoucher type not found!");
                config.set("voucher.cannot-place", "&cYou cannot place vouchers!");
                config.set("voucher.dropped-warning", "&eWarning: You dropped a voucher! Be careful not to lose it.");
                config.set("voucher.error-using", "&cError using voucher!");
                config.set("voucher.error-finding", "&cError: Could not find voucher in inventory!");

                // Command Messages
                config.set("commands.replaced", "&aCommand replaced: {command}");
                config.set("commands.not-found", "&cCommand not found: {id}");
                config.set("commands.testing", "&6Testing command: {id} for player: {player}");
                config.set("commands.executing", "&7Executing: &f{command}");

                // GUI Messages
                config.set("gui.confirm", "&aConfirm");
                config.set("gui.cancel", "&cCancel");
                config.set("gui.confirm-lore", "&7Click to confirm");
                config.set("gui.cancel-lore", "&7Click to cancel");
                config.set("gui.back", "&7Back");
                config.set("gui.back-lore", "&7Click to go back");
                config.set("gui.save", "&aSave");
                config.set("gui.save-lore", "&7Click to save");

                config.save(messagesFile);
                plugin.getLogger().info("Created default messages.yml");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default messages.yml: " + e.getMessage());
        }
    }

    public void loadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null || message.isEmpty()) {
            return null;
        }
        return ColorUtils.colorize(message);
    }

    public void sendMessage(CommandSender sender, String key) {
        String message = getMessage(key);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendMessage(CommandSender sender, String key, String placeholder, String value) {
        String message = getMessage(key);
        if (message != null && !message.isEmpty()) {
            message = message.replace("{" + placeholder + "}", value);
            sender.sendMessage(message);
        }
    }

    public void sendMessage(CommandSender sender, String key, String... replacements) {
        String message = getMessage(key);
        if (message != null && !message.isEmpty()) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
            sender.sendMessage(message);
        }
    }

    public List<String> getMessageList(String key) {
        List<String> messages = messagesConfig.getStringList(key);
        if (messages.isEmpty()) {
            return null;
        }
        return ColorUtils.colorizeList(messages);
    }

    public void sendMessageList(CommandSender sender, String key) {
        List<String> messages = getMessageList(key);
        if (messages != null) {
            for (String message : messages) {
                sender.sendMessage(message);
            }
        }
    }
}