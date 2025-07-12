package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoCraftManager {

    private final GhastMiscPlugin plugin;
    private final Map<UUID, Boolean> autoCraftEnabled = new HashMap<>();
    private BukkitRunnable autoCraftTask;

    public AutoCraftManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (autoCraftTask != null) {
            autoCraftTask.cancel();
        }

        autoCraftTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processAutoCraft();
                    processCompactorAutoCraft();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in auto-craft task: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        autoCraftTask.runTaskTimer(plugin, 60L, 60L); // Every 3 seconds (60 ticks)
    }

    public void stop() {
        if (autoCraftTask != null) {
            autoCraftTask.cancel();
            autoCraftTask = null;
        }
    }

    public boolean toggleAutocraft(Player player) {
        UUID playerId = player.getUniqueId();
        boolean enabled = !autoCraftEnabled.getOrDefault(playerId, false);

        if (enabled) {
            autoCraftEnabled.put(playerId, true);
        } else {
            autoCraftEnabled.remove(playerId);
        }

        return enabled;
    }

    public boolean performOneCraft(Player player) {
        try {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem == null || heldItem.getType().isAir()) {
                return false;
            }

            // Find recipe that produces this item
            CraftingManager craftingManager = plugin.getCraftingManager();
            Map<String, CraftingManager.CraftingRecipe> recipes = craftingManager.getCustomRecipes();

            for (CraftingManager.CraftingRecipe recipe : recipes.values()) {
                // Check if this recipe produces the held item
                if (ItemUtils.isSimilar(recipe.getResult(), heldItem)) {
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

                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Error in one-craft for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void processAutoCraft() {
        for (UUID playerId : autoCraftEnabled.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                autoCraftForPlayer(player);
            }
        }
    }

    private void processCompactorAutoCraft() {
        // Auto-craft only selected items in each player's compactor
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                plugin.getCompactorManager().processCompactorAutoCrafting(player);
            }
        }
    }

    private void autoCraftForPlayer(Player player) {
        try {
            CraftingManager craftingManager = plugin.getCraftingManager();
            Map<String, CraftingManager.CraftingRecipe> recipes = craftingManager.getCustomRecipes();

            for (CraftingManager.CraftingRecipe recipe : recipes.values()) {
                // Check permission
                if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
                    continue;
                }

                // Check if player has all required ingredients
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
        } catch (Exception e) {
            plugin.getLogger().severe("Error in auto-craft for player " + player.getName() + ": " + e.getMessage());
        }
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
                org.bukkit.Material material = org.bukkit.Material.valueOf(ingredient.getName().toUpperCase());
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
                    org.bukkit.Material material = org.bukkit.Material.valueOf(ingredient.getName().toUpperCase());
                    ItemUtils.removeItems(player.getInventory(), material, totalNeeded);
                } catch (IllegalArgumentException e) {
                    // Invalid material
                }
            }
        }
    }

    public boolean isAutoCraftEnabled(UUID playerId) {
        return autoCraftEnabled.getOrDefault(playerId, false);
    }
}