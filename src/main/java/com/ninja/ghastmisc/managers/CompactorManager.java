package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ItemUtils;
import com.ninja.ghastmisc.utils.ColorUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CompactorManager {

    private final GhastMiscPlugin plugin;
    private final Map<UUID, Map<Integer, String>> playerCompactorItems = new HashMap<>();
    private final Gson gson = new Gson();
    private final File compactorFile;

    public CompactorManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;
        this.compactorFile = new File(plugin.getDataFolder(), "compactor.json");
        loadCompactorData();
    }

    public void loadCompactorData() {
        if (!compactorFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(compactorFile)) {
            Map<String, Map<Integer, String>> data = gson.fromJson(reader,
                    new TypeToken<Map<String, Map<Integer, String>>>(){}.getType());

            if (data != null) {
                playerCompactorItems.clear();
                for (Map.Entry<String, Map<Integer, String>> entry : data.entrySet()) {
                    try {
                        UUID playerId = UUID.fromString(entry.getKey());
                        playerCompactorItems.put(playerId, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in compactor.json: " + entry.getKey());
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading compactor data: " + e.getMessage());
        }
    }

    public void saveCompactorData() {
        try (FileWriter writer = new FileWriter(compactorFile)) {
            Map<String, Map<Integer, String>> data = new HashMap<>();
            for (Map.Entry<UUID, Map<Integer, String>> entry : playerCompactorItems.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue());
            }
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving compactor data: " + e.getMessage());
        }
    }

    public void openCompactorGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 45, "§6Compactor");

        // Fill with glass panes
        ItemStack glassPane = ItemUtils.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, glassPane);
        }

        // Load player's compactor items
        Map<Integer, String> playerItems = playerCompactorItems.getOrDefault(player.getUniqueId(), new HashMap<>());

        // Set compactor slots (centered in GUI)
        int[] compactorSlots = {19, 20, 21, 22, 23, 24, 25}; // 7 slots centered

        for (int i = 0; i < compactorSlots.length; i++) {
            int slot = compactorSlots[i];
            gui.setItem(slot, null); // Clear for player interaction

            // If player has item saved for this slot, display it
            if (playerItems.containsKey(i)) {
                String itemId = playerItems.get(i);
                ItemStack displayItem = getItemFromId(itemId);
                if (displayItem != null) {
                    gui.setItem(slot, displayItem);
                }
            }
        }

        player.openInventory(gui);
        plugin.getMessageManager().sendMessage(player, "crafting.compactor-opened");
    }

    public void handleCompactorClick(Player player, int slot, ItemStack clickedItem) {
        // Check if it's a compactor slot
        int[] compactorSlots = {19, 20, 21, 22, 23, 24, 25};
        int compactorSlot = -1;

        for (int i = 0; i < compactorSlots.length; i++) {
            if (compactorSlots[i] == slot) {
                compactorSlot = i;
                break;
            }
        }

        if (compactorSlot == -1) {
            return; // Not a compactor slot
        }

        UUID playerId = player.getUniqueId();
        Map<Integer, String> playerItems = playerCompactorItems.computeIfAbsent(playerId, k -> new HashMap<>());

        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            // Player clicked on an item in compactor - remove it
            playerItems.remove(compactorSlot);
            player.getOpenInventory().getTopInventory().setItem(slot, null);
            plugin.getMessageManager().sendMessage(player, "crafting.compactor-item-removed");
        } else {
            // Player clicked on empty slot - check if they have an item to add
            // This will be handled by the inventory click event
        }

        saveCompactorData();
    }

    public void handleInventoryClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Check if item has a valid crafting recipe
        String itemId = getItemId(clickedItem);
        if (itemId == null) {
            plugin.getMessageManager().sendMessage(player, "crafting.compactor-invalid-item");
            return;
        }

        // Check if recipe exists
        if (!hasValidRecipe(itemId)) {
            plugin.getMessageManager().sendMessage(player, "crafting.compactor-no-recipe");
            return;
        }

        // Find empty slot in compactor
        UUID playerId = player.getUniqueId();
        Map<Integer, String> playerItems = playerCompactorItems.computeIfAbsent(playerId, k -> new HashMap<>());

        int emptySlot = -1;
        for (int i = 0; i < 7; i++) {
            if (!playerItems.containsKey(i)) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot == -1) {
            plugin.getMessageManager().sendMessage(player, "crafting.compactor-full");
            return;
        }

        // Add item to compactor
        playerItems.put(emptySlot, itemId);

        // Update GUI
        int[] compactorSlots = {19, 20, 21, 22, 23, 24, 25};
        player.getOpenInventory().getTopInventory().setItem(compactorSlots[emptySlot], clickedItem.clone());

        plugin.getMessageManager().sendMessage(player, "crafting.compactor-item-added");
        saveCompactorData();
    }

    public void processCompactorCrafting(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Integer, String> playerItems = playerCompactorItems.getOrDefault(playerId, new HashMap<>());

        if (playerItems.isEmpty()) {
            return;
        }

        CraftingManager craftingManager = plugin.getCraftingManager();
        Map<String, CraftingManager.CraftingRecipe> recipes = craftingManager.getCustomRecipes();

        for (String itemId : playerItems.values()) {
            // Find recipe for this item
            CraftingManager.CraftingRecipe recipe = recipes.get(itemId);
            if (recipe == null) {
                continue;
            }

            // Check permission
            if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                continue;
            }

            // Check if player has ingredients
            int maxCraftable = getMaxCraftableAmount(player, recipe);
            if (maxCraftable > 0) {
                // Check inventory space
                int spaceForResults = getAvailableInventorySpace(player, recipe.getResult());
                int craftAmount = Math.min(maxCraftable, spaceForResults);

                if (craftAmount > 0) {
                    // Remove ingredients
                    removeIngredientsForCraft(player, recipe, craftAmount);

                    // Give result
                    ItemStack result = recipe.getResult().clone();
                    result.setAmount(craftAmount);
                    player.getInventory().addItem(result);
                }
            }
        }
    }

    private String getItemId(ItemStack item) {
        // Check if it's a custom item
        String customId = ItemUtils.getCustomItemId(item);
        if (customId != null) {
            return customId;
        }

        // Check if it's a recipe result
        String recipeId = ItemUtils.getRecipeId(item);
        if (recipeId != null) {
            return recipeId;
        }

        // Check if it's a vanilla item with recipe
        if (!ItemUtils.isCustomItem(item)) {
            return item.getType().name().toLowerCase();
        }

        return null;
    }

    private ItemStack getItemFromId(String itemId) {
        CraftingManager craftingManager = plugin.getCraftingManager();

        // Check custom ingredients
        Map<String, ItemStack> customIngredients = craftingManager.getCustomIngredients();
        if (customIngredients.containsKey(itemId)) {
            return customIngredients.get(itemId).clone();
        }

        // Check recipes
        Map<String, CraftingManager.CraftingRecipe> recipes = craftingManager.getCustomRecipes();
        if (recipes.containsKey(itemId)) {
            return recipes.get(itemId).getResult().clone();
        }

        // Check vanilla material
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean hasValidRecipe(String itemId) {
        CraftingManager craftingManager = plugin.getCraftingManager();
        Map<String, CraftingManager.CraftingRecipe> recipes = craftingManager.getCustomRecipes();

        return recipes.containsKey(itemId);
    }

    private int getMaxCraftableAmount(Player player, CraftingManager.CraftingRecipe recipe) {
        int maxCraftable = Integer.MAX_VALUE;
        CraftingManager craftingManager = plugin.getCraftingManager();

        for (Map.Entry<Integer, CraftingManager.RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
            CraftingManager.RecipeIngredient ingredient = entry.getValue();
            int available = countAvailableIngredient(player, ingredient);
            int needed = ingredient.getAmount();

            if (available < needed) {
                return 0;
            }

            maxCraftable = Math.min(maxCraftable, available / needed);
        }

        return maxCraftable;
    }

    private int countAvailableIngredient(Player player, CraftingManager.RecipeIngredient ingredient) {
        int count = 0;
        CraftingManager craftingManager = plugin.getCraftingManager();

        if (ingredient.isCustom()) {
            Map<String, ItemStack> customIngredients = craftingManager.getCustomIngredients();
            ItemStack customItem = customIngredients.get(ingredient.getName());
            if (customItem != null) {
                count = ItemUtils.countSimilarItems(player.getInventory(), customItem);
            }
        } else {
            try {
                Material material = Material.valueOf(ingredient.getName().toUpperCase());
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material && !ItemUtils.isCustomItem(item)) {
                        count += item.getAmount();
                    }
                }
            } catch (IllegalArgumentException e) {
                // Invalid material
            }
        }

        return count;
    }

    private int getAvailableInventorySpace(Player player, ItemStack resultItem) {
        int availableSpace = 0;

        // Check existing stacks that can be expanded
        for (ItemStack item : player.getInventory().getContents()) {
            if (ItemUtils.isSimilar(item, resultItem)) {
                availableSpace += item.getMaxStackSize() - item.getAmount();
            }
        }

        // Check empty slots
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                availableSpace += resultItem.getMaxStackSize();
            }
        }

        return availableSpace;
    }
    public void processCompactorAutoCrafting(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Integer, String> playerItems = playerCompactorItems.getOrDefault(playerId, new HashMap<>());

        if (playerItems.isEmpty()) {
            return;
        }

        CraftingManager craftingManager = plugin.getCraftingManager();
        Map<String, CraftingManager.CraftingRecipe> recipes = craftingManager.getCustomRecipes();

        // Only process items that are selected in the compactor
        for (String itemId : playerItems.values()) {
            // Find recipe for this specific selected item
            CraftingManager.CraftingRecipe recipe = recipes.get(itemId);
            if (recipe == null) {
                continue;
            }

            // Check permission
            if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                continue;
            }

            // Check if player has ingredients for this specific recipe
            int maxCraftable = getMaxCraftableAmount(player, recipe);
            if (maxCraftable > 0) {
                // Check inventory space
                int spaceForResults = getAvailableInventorySpace(player, recipe.getResult());
                int craftAmount = Math.min(maxCraftable, spaceForResults);

                if (craftAmount > 0) {
                    // Remove ingredients
                    removeIngredientsForCraft(player, recipe, craftAmount);

                    // Give result
                    ItemStack result = recipe.getResult().clone();
                    result.setAmount(craftAmount);
                    player.getInventory().addItem(result);
                }
            }
        }
    }

    private void removeIngredientsForCraft(Player player, CraftingManager.CraftingRecipe recipe, int craftAmount) {
        CraftingManager craftingManager = plugin.getCraftingManager();

        for (Map.Entry<Integer, CraftingManager.RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
            CraftingManager.RecipeIngredient ingredient = entry.getValue();
            int totalNeeded = ingredient.getAmount() * craftAmount;

            if (ingredient.isCustom()) {
                Map<String, ItemStack> customIngredients = craftingManager.getCustomIngredients();
                ItemStack customItem = customIngredients.get(ingredient.getName());
                if (customItem != null) {
                    ItemUtils.removeItems(player.getInventory(), customItem, totalNeeded);
                }
            } else {
                try {
                    Material material = Material.valueOf(ingredient.getName().toUpperCase());
                    ItemUtils.removeItems(player.getInventory(), material, totalNeeded);
                } catch (IllegalArgumentException e) {
                    // Invalid material
                }
            }
        }
    }
}