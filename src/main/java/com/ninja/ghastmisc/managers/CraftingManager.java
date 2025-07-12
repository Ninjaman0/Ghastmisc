package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ItemUtils;
import com.ninja.ghastmisc.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class CraftingManager {
    private final GhastMiscPlugin plugin;
    private final Map<String, ItemStack> customIngredients = new HashMap<>();
    private final Map<String, CraftingRecipe> customRecipes = new HashMap<>();

    public CraftingManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;

        // Verify config is loaded
        if (plugin.getConfigManager().getCraftingConfig() == null) {
            plugin.getLogger().severe("Failed to load crafting config! Disabling...");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        loadCraftingData();
    }

    public void loadCraftingData() {
        customIngredients.clear();
        customRecipes.clear();

        ConfigurationSection ingredients = plugin.getConfigManager()
                .getCraftingConfig()
                .getConfigurationSection("ingredients");

        ConfigurationSection recipes = plugin.getConfigManager()
                .getCraftingConfig()
                .getConfigurationSection("recipes");

        // Load ingredients with null checks
        if (ingredients != null) {
            for (String key : ingredients.getKeys(false)) {
                ConfigurationSection ingredient = ingredients.getConfigurationSection(key);
                if (ingredient != null) {
                    ItemStack item = ItemUtils.createCustomItem(ingredient);
                    if (item != null) {
                        customIngredients.put(key, item);
                    }
                }
            }
        }

        // Load recipes with null checks
        if (recipes != null) {
            for (String key : recipes.getKeys(false)) {
                ConfigurationSection recipe = recipes.getConfigurationSection(key);
                if (recipe != null) {
                    CraftingRecipe craftingRecipe = loadRecipe(key, recipe);
                    if (craftingRecipe != null) {
                        customRecipes.put(key, craftingRecipe);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + customIngredients.size() + " ingredients and " +
                customRecipes.size() + " recipes");
    }

    private CraftingRecipe loadRecipe(String id, ConfigurationSection config) {
        try {
            String itemName = config.getString("itemname", "");
            Material material = Material.valueOf(config.getString("material", "STONE"));
            List<String> lore = config.getStringList("lore");
            String permission = config.getString("permission");
            boolean glow = config.getBoolean("glow", false);
            boolean noVanilla = config.getBoolean("no-vanilla", false);
            int customModelData = config.getInt("custom_model_data", 0);

            ConfigurationSection flags = config.getConfigurationSection("flags");
            ConfigurationSection recipeSection = config.getConfigurationSection("recipe");
            ConfigurationSection effects = config.getConfigurationSection("effects");

            if (recipeSection == null) {
                return null;
            }

            Map<Integer, RecipeIngredient> recipeIngredients = new HashMap<>();
            for (String slotStr : recipeSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    if (slot >= 1 && slot <= 9) {
                        String ingredientData = recipeSection.getString(slotStr);
                        RecipeIngredient ingredient = parseIngredient(ingredientData);
                        if (ingredient != null) {
                            recipeIngredients.put(slot, ingredient);
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot number in recipe " + id + ": " + slotStr);
                }
            }

            ItemStack resultItem = ItemUtils.createCustomItemFromRecipe(config);

            return new CraftingRecipe(id, resultItem, recipeIngredients, permission, noVanilla, effects);

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading recipe " + id + ": " + e.getMessage());
            return null;
        }
    }

    private RecipeIngredient parseIngredient(String ingredientData) {
        String[] parts = ingredientData.split(":");
        if (parts.length != 2) {
            return null;
        }

        String ingredientName = parts[0];
        int amount;

        try {
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        // Check if it's a custom ingredient
        if (customIngredients.containsKey(ingredientName)) {
            return new RecipeIngredient(ingredientName, amount, true);
        }

        // Check if it's a vanilla material
        try {
            Material material = Material.valueOf(ingredientName.toUpperCase());
            return new RecipeIngredient(ingredientName, amount, false);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void openCraftingGUI(Player player) {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();

        int size = guiConfig.getInt("crafting-gui.size", 45);
        String title = ColorUtils.colorize(guiConfig.getString("crafting-gui.title", "§6Custom Crafting Table"));

        Inventory gui = Bukkit.createInventory(player, size, title);

        // Load background items
        ConfigurationSection backgroundSection = guiConfig.getConfigurationSection("crafting-gui.background");
        if (backgroundSection != null) {
            for (String slotStr : backgroundSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    if (slot >= 0 && slot < size) {
                        ConfigurationSection itemSection = backgroundSection.getConfigurationSection(slotStr);
                        if (itemSection != null) {
                            ItemStack item = ItemUtils.createCustomItem(itemSection);
                            if (item != null) {
                                gui.setItem(slot, item);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot in GUI background: " + slotStr);
                }
            }
        }

        // Load additional items
        ConfigurationSection additionalSection = guiConfig.getConfigurationSection("crafting-gui.additional-items");
        if (additionalSection != null) {
            for (String itemKey : additionalSection.getKeys(false)) {
                ConfigurationSection itemSection = additionalSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    int slot = itemSection.getInt("slot", -1);
                    if (slot >= 0 && slot < size) {
                        ItemStack item = createSpecialItem(itemSection);
                        if (item != null) {
                            gui.setItem(slot, item);
                        }
                    }
                }
            }
        }

        // Set default glass panes for non-configured slots
        ItemStack glassPane = ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < size; i++) {
            if (gui.getItem(i) == null && !isCraftingSlot(i) && !isResultSlot(i)) {
                gui.setItem(i, glassPane);
            }
        }

        player.openInventory(gui);
        plugin.getMessageManager().sendMessage(player, "crafting.gui-opened");
    }

    private ItemStack createSpecialItem(ConfigurationSection config) {
        String materialName = config.getString("material", "STONE");
        Material material = Material.valueOf(materialName.toUpperCase());

        ItemStack item = new ItemStack(material);

        // Handle player head with texture
        if (material == Material.PLAYER_HEAD) {
            String texture = config.getString("texture");
            if (texture != null && !texture.isEmpty()) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    ItemUtils.setSkullTexture(meta, texture);
                    item.setItemMeta(meta);
                }
            }
        }

        // Apply other properties
        ItemStack finalItem = ItemUtils.createCustomItem(config);
        return finalItem != null ? finalItem : item;
    }

    private boolean isCraftingSlot(int slot) {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> craftingSlots = guiConfig.getIntegerList("crafting-gui.crafting-slots");

        if (craftingSlots.isEmpty()) {
            // Default crafting slots
            return (slot >= 10 && slot <= 12) || (slot >= 19 && slot <= 21) || (slot >= 28 && slot <= 30);
        }

        return craftingSlots.contains(slot);
    }

    private boolean isResultSlot(int slot) {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
        int resultSlot = guiConfig.getInt("crafting-gui.result-slot", 24);
        return slot == resultSlot;
    }

    public void processCompactor(Player player, ItemStack[] items) {
        try {
            // Process using the CompactorManager
            plugin.getCompactorManager().processCompactorCrafting(player);
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing compactor: " + e.getMessage());
            player.sendMessage("§cError processing compactor!");
        }
    }

    public void listRecipes(CommandSender sender) {
        sender.sendMessage("§6Custom Recipes:");
        for (String recipeId : customRecipes.keySet()) {
            CraftingRecipe recipe = customRecipes.get(recipeId);
            sender.sendMessage("§7- §f" + recipeId + " §8(§f" + recipe.getResult().getType() + "§8)");
        }
    }

    public void listIngredients(CommandSender sender) {
        sender.sendMessage("§6Custom Ingredients:");
        for (String ingredientId : customIngredients.keySet()) {
            ItemStack ingredient = customIngredients.get(ingredientId);
            sender.sendMessage("§7- §f" + ingredientId + " §8(§f" + ingredient.getType() + "§8)");
        }
    }

    public void registerItem(Player player, String itemId) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to register it!");
            return;
        }

        // Add to crafting config
        ConfigurationSection ingredients = plugin.getConfigManager().getCraftingConfig().getConfigurationSection("ingredients");
        if (ingredients == null) {
            ingredients = plugin.getConfigManager().getCraftingConfig().createSection("ingredients");
        }

        ConfigurationSection itemSection = ingredients.createSection(itemId);
        ItemUtils.saveItemToConfig(item, itemSection);

        plugin.getConfigManager().saveCraftingConfig();
        loadCraftingData();

        plugin.getMessageManager().sendMessage(player, "crafting.item-registered", "id", itemId);
    }

    public void giveItem(CommandSender sender, Player player, String itemId, int amount) {
        if (customIngredients.containsKey(itemId)) {
            ItemStack item = customIngredients.get(itemId).clone();
            item.setAmount(Math.min(amount, 64));

            if (ItemUtils.hasInventorySpace(player.getInventory(), item)) {
                player.getInventory().addItem(item);
                plugin.getMessageManager().sendMessage(sender, "crafting.item-given",
                        "amount", String.valueOf(amount), "item", itemId, "player", player.getName());
            } else {
                plugin.getMessageManager().sendMessage(sender, "crafting.inventory-full");
            }
        } else if (customRecipes.containsKey(itemId)) {
            ItemStack item = customRecipes.get(itemId).getResult().clone();
            item.setAmount(Math.min(amount, 64));

            if (ItemUtils.hasInventorySpace(player.getInventory(), item)) {
                player.getInventory().addItem(item);
                plugin.getMessageManager().sendMessage(sender, "crafting.item-given",
                        "amount", String.valueOf(amount), "item", itemId, "player", player.getName());
            } else {
                plugin.getMessageManager().sendMessage(sender, "crafting.inventory-full");
            }
        } else {
            plugin.getMessageManager().sendMessage(sender, "crafting.item-not-found", "id", itemId);
        }
    }

    public void takeItem(CommandSender sender, Player player, String itemId, int amount) {
        if (customIngredients.containsKey(itemId)) {
            ItemStack item = customIngredients.get(itemId);
            int removed = ItemUtils.removeItems(player.getInventory(), item, amount);
            plugin.getMessageManager().sendMessage(sender, "crafting.item-taken",
                    "amount", String.valueOf(removed), "item", itemId, "player", player.getName());
        } else if (customRecipes.containsKey(itemId)) {
            ItemStack item = customRecipes.get(itemId).getResult();
            int removed = ItemUtils.removeItems(player.getInventory(), item, amount);
            plugin.getMessageManager().sendMessage(sender, "crafting.item-taken",
                    "amount", String.valueOf(removed), "item", itemId, "player", player.getName());
        } else {
            plugin.getMessageManager().sendMessage(sender, "crafting.item-not-found", "id", itemId);
        }
    }

    public void viewRecipe(Player player, String itemId) {
        if (!customRecipes.containsKey(itemId)) {
            plugin.getMessageManager().sendMessage(player, "crafting.recipe-not-found", "id", itemId);
            return;
        }

        CraftingRecipe recipe = customRecipes.get(itemId);
        Inventory gui = Bukkit.createInventory(player, 45, "§6Recipe: " + itemId);

        // Fill with glass panes first
        ItemStack glassPane = ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, glassPane);
        }

        // Add recipe items to GUI
        for (Map.Entry<Integer, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
            int slot = convertSlotToGUI(entry.getKey());
            RecipeIngredient ingredient = entry.getValue();

            ItemStack item;
            if (ingredient.isCustom()) {
                item = customIngredients.get(ingredient.getName());
            } else {
                item = new ItemStack(Material.valueOf(ingredient.getName().toUpperCase()));
            }

            if (item != null) {
                item = item.clone();
                item.setAmount(ingredient.getAmount());
                gui.setItem(slot, item);
            }
        }

        // Add result item
        gui.setItem(24, recipe.getResult());

        player.openInventory(gui);
    }

    public void openRecipeEditor(Player player, String itemId) {
        Inventory gui = Bukkit.createInventory(player, 45, "§6Edit Recipe: " + itemId);

        // Load existing recipe if it exists
        CraftingRecipe recipe = customRecipes.get(itemId);
        if (recipe != null) {
            // Add existing recipe items to GUI
            for (Map.Entry<Integer, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
                int slot = convertSlotToGUI(entry.getKey());
                RecipeIngredient ingredient = entry.getValue();

                ItemStack item;
                if (ingredient.isCustom()) {
                    item = customIngredients.get(ingredient.getName());
                } else {
                    item = new ItemStack(Material.valueOf(ingredient.getName().toUpperCase()));
                }

                if (item != null) {
                    item = item.clone();
                    item.setAmount(ingredient.getAmount());
                    gui.setItem(slot, item);
                }
            }

            // Add result item
            gui.setItem(24, recipe.getResult());
        }

        // Add control buttons
        ItemStack cancelButton = ItemUtils.createGuiItem(Material.RED_CONCRETE,
                plugin.getMessageManager().getMessage("gui.cancel") != null ?
                        plugin.getMessageManager().getMessage("gui.cancel") : "§cCancel",
                plugin.getMessageManager().getMessage("gui.cancel-lore") != null ?
                        plugin.getMessageManager().getMessage("gui.cancel-lore") : "§7Click to cancel editing");
        gui.setItem(36, cancelButton);

        ItemStack saveButton = ItemUtils.createGuiItem(Material.LIME_CONCRETE,
                plugin.getMessageManager().getMessage("gui.save") != null ?
                        plugin.getMessageManager().getMessage("gui.save") : "§aSave",
                plugin.getMessageManager().getMessage("gui.save-lore") != null ?
                        plugin.getMessageManager().getMessage("gui.save-lore") : "§7Click to save recipe");
        gui.setItem(44, saveButton);

        // Fill other slots with glass panes
        ItemStack filler = ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 45; i++) {
            if (gui.getItem(i) == null && !isCraftingSlotInEditor(i) && i != 24 && i != 36 && i != 44) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    private boolean isCraftingSlotInEditor(int slot) {
        return (slot >= 10 && slot <= 12) || (slot >= 19 && slot <= 21) || (slot >= 28 && slot <= 30);
    }

    private int convertSlotToGUI(int craftingSlot) {
        // Convert crafting slot (1-9) to GUI slot
        switch (craftingSlot) {
            case 1: return 10;
            case 2: return 11;
            case 3: return 12;
            case 4: return 19;
            case 5: return 20;
            case 6: return 21;
            case 7: return 28;
            case 8: return 29;
            case 9: return 30;
            default: return 0;
        }
    }

    public Set<String> getRecipeIds() {
        return new HashSet<>(customRecipes.keySet());
    }

    public Map<String, ItemStack> getCustomIngredients() {
        return new HashMap<>(customIngredients);
    }

    public Map<String, CraftingRecipe> getCustomRecipes() {
        return new HashMap<>(customRecipes);
    }

    // Inner classes for recipe data
    public static class CraftingRecipe {
        private final String id;
        private final ItemStack result;
        private final Map<Integer, RecipeIngredient> ingredients;
        private final String permission;
        private final boolean noVanilla;
        private final ConfigurationSection effects;

        public CraftingRecipe(String id, ItemStack result, Map<Integer, RecipeIngredient> ingredients,
                              String permission, boolean noVanilla, ConfigurationSection effects) {
            this.id = id;
            this.result = result;
            this.ingredients = ingredients;
            this.permission = permission;
            this.noVanilla = noVanilla;
            this.effects = effects;
        }

        // Getters
        public String getId() { return id; }
        public ItemStack getResult() { return result; }
        public Map<Integer, RecipeIngredient> getIngredients() { return ingredients; }
        public String getPermission() { return permission; }
        public boolean isNoVanilla() { return noVanilla; }
        public ConfigurationSection getEffects() { return effects; }
    }

    public static class RecipeIngredient {
        private final String name;
        private final int amount;
        private final boolean custom;

        public RecipeIngredient(String name, int amount, boolean custom) {
            this.name = name;
            this.amount = amount;
            this.custom = custom;
        }

        // Getters
        public String getName() { return name; }
        public int getAmount() { return amount; }
        public boolean isCustom() { return custom; }
    }
}