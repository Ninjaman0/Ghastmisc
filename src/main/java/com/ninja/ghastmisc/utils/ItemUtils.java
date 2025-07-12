package com.ninja.ghastmisc.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.Bukkit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtils {

    private static JavaPlugin plugin;
    private static NamespacedKey CUSTOM_ITEM_KEY;
    private static NamespacedKey ITEM_ID_KEY;
    private static NamespacedKey RECIPE_RESULT_KEY;
    private static NamespacedKey RECIPE_ID_KEY;
    private static NamespacedKey VOUCHER_TYPE_KEY;
    private static NamespacedKey VOUCHER_TIME_KEY;
    private static NamespacedKey VOUCHER_MULTIPLIER_KEY;

    public static void initialize(JavaPlugin plugin) {
        ItemUtils.plugin = plugin;
        CUSTOM_ITEM_KEY = new NamespacedKey(plugin, "custom_item");
        ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
        RECIPE_RESULT_KEY = new NamespacedKey(plugin, "recipe_result");
        RECIPE_ID_KEY = new NamespacedKey(plugin, "recipe_id");
        VOUCHER_TYPE_KEY = new NamespacedKey(plugin, "voucher_type");
        VOUCHER_TIME_KEY = new NamespacedKey(plugin, "voucher_time");
        VOUCHER_MULTIPLIER_KEY = new NamespacedKey(plugin, "voucher_multiplier");
    }

    public static ItemStack createCustomItem(ConfigurationSection config) {
        try {
            String materialName = config.getString("material", "STONE");
            Material material = Material.valueOf(materialName.toUpperCase());

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                return null;
            }

            // Set display name with color support
            String itemName = config.getString("item-name", "");
            if (!itemName.isEmpty()) {
                meta.setDisplayName(ColorUtils.colorize(itemName));
            }

            // Set lore with color support
            List<String> lore = config.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }

            // Set custom model data
            int customModelData = config.getInt("custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            // Handle player head texture
            if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
                String texture = config.getString("texture");
                if (texture != null && !texture.isEmpty()) {
                    setSkullTexture((SkullMeta) meta, texture);
                }
            }

            // Set glow effect
            boolean glow = config.getBoolean("glow", false);
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Set item flags
            ConfigurationSection flags = config.getConfigurationSection("flags");
            if (flags != null) {
                if (flags.getBoolean("hide_attributes", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                }
                if (flags.getBoolean("hide_dye", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_DYE);
                }
                if (flags.getBoolean("hide_enchants", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                if (flags.getBoolean("hide_destroys", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                }
                if (flags.getBoolean("hide_placed_on", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
                }
                if (flags.getBoolean("hide_unbreakable", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                }
            }

            // Add custom data using PersistentDataContainer
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(CUSTOM_ITEM_KEY, PersistentDataType.STRING, "true");
            container.set(ITEM_ID_KEY, PersistentDataType.STRING, config.getName());

            item.setItemMeta(meta);
            return item;

        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().severe("Error creating custom item: " + e.getMessage());
            }
            return null;
        }
    }

    public static ItemStack createCustomItemFromRecipe(ConfigurationSection config) {
        try {
            String materialName = config.getString("material", "STONE");
            Material material = Material.valueOf(materialName.toUpperCase());

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                return null;
            }

            // Set display name with color support
            String itemName = config.getString("itemname", "");
            if (!itemName.isEmpty()) {
                meta.setDisplayName(ColorUtils.colorize(itemName));
            }

            // Set lore with color support
            List<String> lore = config.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }

            // Set custom model data
            int customModelData = config.getInt("custom_model_data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            // Set glow effect
            boolean glow = config.getBoolean("glow", false);
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Set item flags
            ConfigurationSection flags = config.getConfigurationSection("flags");
            if (flags != null) {
                if (flags.getBoolean("hide_attributes", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                }
                if (flags.getBoolean("hide_dye", false)) {
                    meta.addItemFlags(ItemFlag.HIDE_DYE);
                }
            }

            // Add custom data using PersistentDataContainer
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(RECIPE_RESULT_KEY, PersistentDataType.STRING, "true");
            container.set(RECIPE_ID_KEY, PersistentDataType.STRING, config.getName());

            item.setItemMeta(meta);
            return item;

        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().severe("Error creating custom recipe item: " + e.getMessage());
            }
            return null;
        }
    }

    public static ItemStack createGuiItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (!lore.isEmpty()) {
                List<String> loreList = new ArrayList<>();
                loreList.add(ColorUtils.colorize(lore));
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public static void setDisplayName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            item.setItemMeta(meta);
        }
    }

    public static void setSkullTexture(SkullMeta meta, String textureUrl) {
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            URL url = new URL(textureUrl);
            textures.setSkin(url);
            profile.setTextures(textures);

            meta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            // Fallback to default head
            meta.setDisplayName("§cInvalid Texture");
        }
    }

    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }

        if (item1.getType() != item2.getType()) {
            return false;
        }

        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        if (meta1 == null && meta2 == null) {
            return true;
        }

        if (meta1 == null || meta2 == null) {
            return false;
        }

        // Check custom item IDs first
        String id1 = getCustomItemId(item1);
        String id2 = getCustomItemId(item2);

        if (id1 != null && id2 != null) {
            return id1.equals(id2);
        }

        // Compare display names
        if (!compareStrings(meta1.getDisplayName(), meta2.getDisplayName())) {
            return false;
        }

        // Compare lore
        List<String> lore1 = meta1.getLore();
        List<String> lore2 = meta2.getLore();

        if (lore1 == null && lore2 == null) {
            return true;
        }

        if (lore1 == null || lore2 == null) {
            return false;
        }

        if (lore1.size() != lore2.size()) {
            return false;
        }

        for (int i = 0; i < lore1.size(); i++) {
            if (!lore1.get(i).equals(lore2.get(i))) {
                return false;
            }
        }

        // Compare custom model data
        if (meta1.hasCustomModelData() != meta2.hasCustomModelData()) {
            return false;
        }

        if (meta1.hasCustomModelData() && meta1.getCustomModelData() != meta2.getCustomModelData()) {
            return false;
        }

        return true;
    }

    private static boolean compareStrings(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }

    public static int countSimilarItems(PlayerInventory inventory, ItemStack targetItem) {
        int count = 0;

        for (ItemStack item : inventory.getContents()) {
            if (isSimilar(item, targetItem)) {
                count += item.getAmount();
            }
        }

        return count;
    }

    public static int removeItems(PlayerInventory inventory, ItemStack targetItem, int amount) {
        if (targetItem == null || targetItem.getType() == Material.AIR || amount <= 0) {
            return 0;
        }

        int remaining = amount;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isSimilar(item, targetItem)) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    inventory.setItem(i, null);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }

                if (remaining <= 0) {
                    break;
                }
            }
        }

        return amount - remaining;
    }

    public static int removeItems(PlayerInventory inventory, Material material, int amount) {
        if (material == null || material == Material.AIR || amount <= 0) {
            return 0;
        }

        int remaining = amount;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                // Check if it's not a custom item
                if (!isCustomItem(item)) {
                    int itemAmount = item.getAmount();
                    if (itemAmount <= remaining) {
                        inventory.setItem(i, null);
                        remaining -= itemAmount;
                    } else {
                        item.setAmount(itemAmount - remaining);
                        remaining = 0;
                    }

                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }

        return amount - remaining;
    }

    public static boolean hasInventorySpace(PlayerInventory inventory, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }

        // Check if the item can be added without dropping
        int remaining = item.getAmount();

        // First, check for existing stacks that can be added to
        for (ItemStack invItem : inventory.getContents()) {
            if (invItem != null && isSimilar(invItem, item)) {
                int maxStackSize = invItem.getMaxStackSize();
                int availableSpace = maxStackSize - invItem.getAmount();
                if (availableSpace > 0) {
                    remaining -= availableSpace;
                    if (remaining <= 0) {
                        return true;
                    }
                }
            }
        }

        // Check for empty slots
        for (ItemStack invItem : inventory.getContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) {
                remaining -= item.getMaxStackSize();
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return remaining <= 0;
    }

    public static void saveItemToConfig(ItemStack item, ConfigurationSection config) {
        config.set("material", item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                config.set("item-name", meta.getDisplayName());
            }

            if (meta.hasLore()) {
                config.set("lore", meta.getLore());
            }

            if (meta.hasCustomModelData()) {
                config.set("custom-model-data", meta.getCustomModelData());
            }

            if (meta.hasEnchants()) {
                config.set("glow", true);
            }
        }
    }

    public static ItemStack addCustomData(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            container.set(namespacedKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String getCustomData(ItemStack item, String key) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return container.get(namespacedKey, PersistentDataType.STRING);
    }

    public static boolean hasCustomData(ItemStack item, String key) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return container.has(namespacedKey, PersistentDataType.STRING);
    }

    public static boolean isCustomItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(CUSTOM_ITEM_KEY, PersistentDataType.STRING);
    }

    public static boolean isCustomItem(ItemStack item, String itemId) {
        if (item == null) return false;

        String id = getCustomItemId(item);
        return itemId.equals(id);
    }

    public static String getCustomItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public static boolean isCustomRecipeResult(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(RECIPE_RESULT_KEY, PersistentDataType.STRING);
    }

    public static String getRecipeId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(RECIPE_ID_KEY, PersistentDataType.STRING);
    }

    public static void setVoucherData(ItemStack item, String voucherType, String time, String multiplier) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(VOUCHER_TYPE_KEY, PersistentDataType.STRING, voucherType);
            container.set(VOUCHER_TIME_KEY, PersistentDataType.STRING, time);
            container.set(VOUCHER_MULTIPLIER_KEY, PersistentDataType.STRING, multiplier);
            item.setItemMeta(meta);
        }
    }

    public static String getVoucherType(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(VOUCHER_TYPE_KEY, PersistentDataType.STRING);
    }

    public static String getVoucherTime(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(VOUCHER_TIME_KEY, PersistentDataType.STRING);
    }

    public static String getVoucherMultiplier(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(VOUCHER_MULTIPLIER_KEY, PersistentDataType.STRING);
    }

    public static boolean isVoucher(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(VOUCHER_TYPE_KEY, PersistentDataType.STRING);
    }
}