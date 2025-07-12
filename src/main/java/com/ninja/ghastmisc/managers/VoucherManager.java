package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ItemUtils;
import com.ninja.ghastmisc.utils.ColorUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoucherManager {

    private final GhastMiscPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<String, VoucherData> voucherTypes = new HashMap<>();

    public VoucherManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;
        loadVoucherData();
    }

    public void loadVoucherData() {
        voucherTypes.clear();

        ConfigurationSection config = plugin.getConfigManager().getVouchersConfig();

        // Load each voucher type (levels, essence, money)
        for (String section : new String[]{"levels", "essence", "money"}) {
            ConfigurationSection voucherSection = config.getConfigurationSection(section);
            if (voucherSection != null) {
                VoucherData data = loadVoucherType(section, voucherSection);
                if (data != null) {
                    voucherTypes.put(section, data);
                }
            }
        }

        plugin.getLogger().info("Loaded " + voucherTypes.size() + " voucher types");
    }

    private VoucherData loadVoucherType(String type, ConfigurationSection config) {
        try {
            String name = config.getString("voucher-name", "&7Voucher");
            String lore = String.join("\n", config.getStringList("voucher-lore"));
            String material = config.getString("voucher-material", "PAPER");
            String texture = config.getString("texture");
            int customModelData = config.getInt("custom-model-data", 0);
            String placeholder = config.getString("placeholder", "");
            boolean stackable = config.getBoolean("stackable", false);
            boolean glow = config.getBoolean("glow", false);

            // For money section, use none/not-none actions
            ConfigurationSection noneAction = null;
            ConfigurationSection notNoneAction = null;

            if (type.equals("money")) {
                noneAction = config.getConfigurationSection("none");
                notNoneAction = config.getConfigurationSection("not-none");
            } else {
                // For other sections, use true/false actions
                noneAction = config.getConfigurationSection("false-action");
                notNoneAction = config.getConfigurationSection("true-action");
            }

            VoucherAction noneActionData = null;
            VoucherAction notNoneActionData = null;

            if (noneAction != null) {
                noneActionData = new VoucherAction(
                        noneAction.getBoolean("stop", false),
                        noneAction.getString("stop-msg", ""),
                        noneAction.getString("command", "")
                );
            }

            if (notNoneAction != null) {
                notNoneActionData = new VoucherAction(
                        notNoneAction.getBoolean("stop", false),
                        notNoneAction.getString("stop-msg", ""),
                        notNoneAction.getString("command", "")
                );
            }

            return new VoucherData(type, name, lore, material, texture, customModelData,
                    placeholder, stackable, glow, notNoneActionData, noneActionData);

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading voucher type " + type + ": " + e.getMessage());
            return null;
        }
    }

    public void giveVoucher(CommandSender sender, Player player, String section, int time, double multiplier) {
        if (!voucherTypes.containsKey(section)) {
            plugin.getMessageManager().sendMessage(sender, "voucher.type-not-found");
            return;
        }

        VoucherData voucherData = voucherTypes.get(section);
        ItemStack voucher = createVoucher(voucherData, time, multiplier);

        if (voucher == null) {
            sender.sendMessage("§cError creating voucher!");
            return;
        }

        // Check if player has space in inventory
        if (!ItemUtils.hasInventorySpace(player.getInventory(), voucher)) {
            plugin.getMessageManager().sendMessage(sender, "crafting.inventory-full");
            return;
        }

        // Add voucher to player inventory
        player.getInventory().addItem(voucher);

        plugin.getMessageManager().sendMessage(sender, "voucher.given",
                "type", section, "player", player.getName(), "time", String.valueOf(time),
                "multiplier", String.valueOf(multiplier));


    }

    private ItemStack createVoucher(VoucherData data, int time, double multiplier) {
        try {
            ConfigurationSection itemConfig = plugin.getConfigManager().getVouchersConfig().createSection("temp");

            // Replace placeholders in name and lore
            String name = data.getName()
                    .replace("{time}", String.valueOf(time))
                    .replace("{multiplier}", String.valueOf(multiplier));

            String lore = data.getLore()
                    .replace("{time}", String.valueOf(time))
                    .replace("{multiplier}", String.valueOf(multiplier));

            itemConfig.set("item-name", name);
            itemConfig.set("lore", java.util.Arrays.asList(lore.split("\n")));
            itemConfig.set("material", data.getMaterial());
            itemConfig.set("texture", data.getTexture());
            itemConfig.set("custom-model-data", data.getCustomModelData());
            itemConfig.set("glow", data.isGlow());

            // Create the voucher item
            ItemStack voucher = ItemUtils.createCustomItem(itemConfig);
            if (voucher != null) {
                // Add voucher-specific data
                ItemUtils.setVoucherData(voucher, data.getType(), String.valueOf(time), String.valueOf(multiplier));
            }

            // Clean up temp section
            plugin.getConfigManager().getVouchersConfig().set("temp", null);

            return voucher;

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating voucher: " + e.getMessage());
            return null;
        }
    }

    public boolean handleVoucherUse(Player player, ItemStack voucher) {
        try {
            // Check cooldown
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUse = cooldowns.get(player.getUniqueId());
                if (System.currentTimeMillis() - lastUse < 2000) { // 2 second cooldown
                    return false;
                }
            }

            // Get voucher data
            String voucherType = ItemUtils.getVoucherType(voucher);
            if (voucherType == null || !voucherTypes.containsKey(voucherType)) {
                return false;
            }

            VoucherData data = voucherTypes.get(voucherType);

            // Check placeholder condition with proper PlaceholderAPI parsing
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String placeholder = data.getPlaceholder();

                // Parse placeholders properly for the specific player
                String parsedPlaceholder = PlaceholderAPI.setPlaceholders(player, placeholder);

                // Double-check placeholder parsing - ensure it's working correctly
                if (parsedPlaceholder.equals(placeholder)) {
                    plugin.getLogger().warning("Placeholder might not be properly parsed for " + player.getName() + ": " + placeholder);

 }

                boolean shouldStop = false;

                // Different logic for money section vs others
                if (voucherType.equals("money")) {
                    // For money: check if parsed placeholder equals "None"
                    boolean isNone = parsedPlaceholder.equalsIgnoreCase("None");

                    if (isNone && data.getFalseAction() != null) {
                        if (data.getFalseAction().isStop()) {
                            player.sendMessage(ColorUtils.colorize(data.getFalseAction().getStopMsg()));
                            shouldStop = true;
                        }
                    } else if (!isNone && data.getTrueAction() != null) {
                        if (data.getTrueAction().isStop()) {
                            player.sendMessage(ColorUtils.colorize(data.getTrueAction().getStopMsg()));
                            shouldStop = true;
                        }
                    }
                } else {
                    // For other vouchers: check if parsed placeholder equals "true"
                    boolean placeholderTrue = parsedPlaceholder.equalsIgnoreCase("true");

                    plugin.getLogger().info("Voucher check for " + player.getName() + ": " + placeholder + " -> " + parsedPlaceholder + " -> " + placeholderTrue);

                    if (placeholderTrue && data.getTrueAction() != null) {
                        if (data.getTrueAction().isStop()) {
                            player.sendMessage(ColorUtils.colorize(data.getTrueAction().getStopMsg()));
                            shouldStop = true;
                        }
                    } else if (!placeholderTrue && data.getFalseAction() != null) {
                        if (data.getFalseAction().isStop()) {
                            player.sendMessage(ColorUtils.colorize(data.getFalseAction().getStopMsg()));
                            shouldStop = true;
                        }
                    }
                }

                if (shouldStop) {
                    return false;
                }
            } else {
                plugin.getLogger().warning("PlaceholderAPI not found! Voucher placeholders will not work properly.");
            }

            // Open confirmation GUI
            openConfirmationGUI(player, voucher, data);

            // Set cooldown
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling voucher use: " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "voucher.error-using");
            return false;
        }
    }

    private void openConfirmationGUI(Player player, ItemStack voucher, VoucherData data) {
        Inventory gui = Bukkit.createInventory(player, 45, "§6Confirm Voucher Use");

        // Lock player inventory - ONLY for voucher confirmation
        plugin.getInventoryLockListener().lockInventory(player.getUniqueId());

        // Add voucher to center
        gui.setItem(22, voucher);

        // Add confirm button (lime concrete)
        ItemStack confirm = ItemUtils.createGuiItem(Material.LIME_CONCRETE,
                plugin.getMessageManager().getMessage("gui.confirm") != null ?
                        plugin.getMessageManager().getMessage("gui.confirm") : "§aConfirm",
                plugin.getMessageManager().getMessage("gui.confirm-lore") != null ?
                        plugin.getMessageManager().getMessage("gui.confirm-lore") : "§7Click to use voucher");
        gui.setItem(20, confirm);

        // Add cancel button (red concrete)
        ItemStack cancel = ItemUtils.createGuiItem(Material.RED_CONCRETE,
                plugin.getMessageManager().getMessage("gui.cancel") != null ?
                        plugin.getMessageManager().getMessage("gui.cancel") : "§cCancel",
                plugin.getMessageManager().getMessage("gui.cancel-lore") != null ?
                        plugin.getMessageManager().getMessage("gui.cancel-lore") : "§7Click to cancel");
        gui.setItem(24, cancel);

        // Fill other slots with black stained glass panes
        ItemStack filler = ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 45; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    public void confirmVoucherUse(Player player, ItemStack voucher) {
        // Unlock player inventory
        plugin.getInventoryLockListener().unlockInventory(player.getUniqueId());

        // Validate voucher before processing
        if (!ItemUtils.isVoucher(voucher)) {
            plugin.getMessageManager().sendMessage(player, "voucher.invalid");
            player.closeInventory();
            return;
        }

        String voucherType = ItemUtils.getVoucherType(voucher);
        String timeStr = ItemUtils.getVoucherTime(voucher);
        String multiplierStr = ItemUtils.getVoucherMultiplier(voucher);

        if (voucherType == null || timeStr == null || multiplierStr == null) {
            plugin.getMessageManager().sendMessage(player, "voucher.invalid-data");
            return;
        }

        VoucherData data = voucherTypes.get(voucherType);
        if (data == null) {
            plugin.getMessageManager().sendMessage(player, "voucher.type-not-found");
            return;
        }

        // Execute the appropriate action based on voucher type
        VoucherAction actionToExecute = null;

        if (voucherType.equals("money")) {
            // For money vouchers, check if placeholder result is "None"
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String placeholder = data.getPlaceholder();
                String parsedPlaceholder = PlaceholderAPI.setPlaceholders(player, placeholder);
                boolean isNone = parsedPlaceholder.equalsIgnoreCase("None");

                actionToExecute = isNone ? data.getFalseAction() : data.getTrueAction();
            } else {
                actionToExecute = data.getFalseAction(); // Default to none action if no PlaceholderAPI
            }
        } else {
            // For other vouchers, use the not-none action (which is stored as falseAction)
            actionToExecute = data.getFalseAction();
        }

        if (actionToExecute != null && !actionToExecute.getCommand().isEmpty()) {
            String command = actionToExecute.getCommand()
                    .replace("%player_name%", player.getName())
                    .replace("{time}", timeStr)
                    .replace("{multiplier}", multiplierStr);

            // Convert time from minutes to seconds for the booster commands
            int timeInMinutes;
            try {
                timeInMinutes = Integer.parseInt(timeStr);
            } catch (NumberFormatException e) {
                plugin.getLogger().severe("Invalid time format in voucher: " + timeStr);
                plugin.getMessageManager().sendMessage(player, "voucher.invalid-data");
                return;
            }

            int timeInSeconds = timeInMinutes * 60; // Convert minutes to seconds

            // Replace the time placeholder with seconds
            command = command.replace(timeStr, String.valueOf(timeInSeconds));

            // Parse PlaceholderAPI placeholders in command for the specific player
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String originalCommand = command;
                command = PlaceholderAPI.setPlaceholders(player, command);

                // Log placeholder parsing for debugging
                if (!originalCommand.equals(command)) {

                }
            }

            String finalCommand = command;


            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            });
        }

        // Remove voucher from inventory
        boolean removed = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && ItemUtils.isVoucher(invItem)) {
                String invVoucherType = ItemUtils.getVoucherType(invItem);
                String invTimeStr = ItemUtils.getVoucherTime(invItem);
                String invMultiplierStr = ItemUtils.getVoucherMultiplier(invItem);

                // Check if this is the same voucher
                if (voucherType.equals(invVoucherType) && timeStr.equals(invTimeStr) && multiplierStr.equals(invMultiplierStr)) {
                    if (data.isStackable() && invItem.getAmount() > 1) {
                        invItem.setAmount(invItem.getAmount() - 1);
                    } else {
                        player.getInventory().setItem(i, null);
                    }
                    removed = true;
                    break;
                }
            }
        }

        if (!removed) {
            plugin.getMessageManager().sendMessage(player, "voucher.error-finding");
            player.closeInventory();
            return;
        }

        plugin.getMessageManager().sendMessage(player, "voucher.used");
        player.closeInventory();
    }

    public void cancelVoucherUse(Player player) {
        // Unlock player inventory
        plugin.getInventoryLockListener().unlockInventory(player.getUniqueId());

        plugin.getMessageManager().sendMessage(player, "voucher.cancelled");
        player.closeInventory();
    }

    public boolean isVoucher(ItemStack item) {
        return ItemUtils.isVoucher(item);
    }

    // Inner classes for voucher data
    public static class VoucherData {
        private final String type;
        private final String name;
        private final String lore;
        private final String material;
        private final String texture;
        private final int customModelData;
        private final String placeholder;
        private final boolean stackable;
        private final boolean glow;
        private final VoucherAction trueAction;
        private final VoucherAction falseAction;

        public VoucherData(String type, String name, String lore, String material, String texture,
                           int customModelData, String placeholder, boolean stackable, boolean glow,
                           VoucherAction trueAction, VoucherAction falseAction) {
            this.type = type;
            this.name = name;
            this.lore = lore;
            this.material = material;
            this.texture = texture;
            this.customModelData = customModelData;
            this.placeholder = placeholder;
            this.stackable = stackable;
            this.glow = glow;
            this.trueAction = trueAction;
            this.falseAction = falseAction;
        }

        // Getters
        public String getType() { return type; }
        public String getName() { return name; }
        public String getLore() { return lore; }
        public String getMaterial() { return material; }
        public String getTexture() { return texture; }
        public int getCustomModelData() { return customModelData; }
        public String getPlaceholder() { return placeholder; }
        public boolean isStackable() { return stackable; }
        public boolean isGlow() { return glow; }
        public VoucherAction getTrueAction() { return trueAction; }
        public VoucherAction getFalseAction() { return falseAction; }
    }

    public static class VoucherAction {
        private final boolean stop;
        private final String stopMsg;
        private final String command;

        public VoucherAction(boolean stop, String stopMsg, String command) {
            this.stop = stop;
            this.stopMsg = stopMsg;
            this.command = command;
        }

        // Getters
        public boolean isStop() { return stop; }
        public String getStopMsg() { return stopMsg; }
        public String getCommand() { return command; }
    }
}