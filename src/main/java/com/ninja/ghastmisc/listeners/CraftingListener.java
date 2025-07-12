package com.ninja.ghastmisc.listeners;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.managers.CraftingManager;
import com.ninja.ghastmisc.utils.ItemUtils;
import com.ninja.ghastmisc.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;

public class CraftingListener implements Listener {

    private final GhastMiscPlugin plugin;
    private final Map<UUID, BukkitRunnable> craftingTasks = new HashMap<>();

    public CraftingListener(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Get dynamic titles from config
        String craftingTitle = ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("crafting-gui.title", "§6Custom Crafting Table"));
        String compactorTitle = ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("compactor-gui.title", "§6Compactor"));

        if (title.equals(craftingTitle)) {
            handleCraftingGUIClick(event, player);
        } else if (title.equals(compactorTitle)) {
            handleCompactorClick(event, player);
        } else if (title.startsWith("§6Recipe: ")) {
            handleRecipeViewClick(event, player);
        } else if (title.startsWith("§6Edit Recipe: ")) {
            handleRecipeEditorClick(event, player);
        }
    }

    private void handleCraftingGUIClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();

        // Check if clicking in the top inventory (crafting GUI)
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            // Allow interaction with crafting slots and result slot only
            if (isCraftingSlot(slot) || isResultSlot(slot)) {
                // Process custom crafting
                if (isResultSlot(slot)) { // Result slot
                    // Handle result click
                    processCraftingResult(event, player);
                } else {
                    // Start continuous scanning when items are placed/removed in crafting slots
                    startCraftingScanning(player);

                    // Update crafting result immediately
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        updateCraftingResult(player, event.getInventory());
                    }, 1L);
                }
                return;
            } else {
                // Cancel interaction with non-crafting slots in the GUI
                event.setCancelled(true);
                return;
            }
        }

        // If clicking in player inventory, allow normal interaction
        // This allows the player to interact with their own inventory freely
    }

    private void startCraftingScanning(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel existing task if any
        BukkitRunnable existingTask = craftingTasks.get(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        // Start new scanning task
        BukkitRunnable scanningTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    craftingTasks.remove(playerId);
                    return;
                }

                String currentTitle = player.getOpenInventory().getTitle();
                String craftingTitle = ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("crafting-gui.title", "§6Custom Crafting Table"));

                if (!currentTitle.equals(craftingTitle)) {
                    this.cancel();
                    craftingTasks.remove(playerId);
                    return;
                }

                // Update crafting result continuously
                updateCraftingResult(player, player.getOpenInventory().getTopInventory());
            }
        };

        scanningTask.runTaskTimer(plugin, 5L, 5L); // Run every 5 ticks (0.25 seconds)
        craftingTasks.put(playerId, scanningTask);
    }

    private void stopCraftingScanning(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = craftingTasks.get(playerId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        craftingTasks.remove(playerId);
    }

    private void handleCompactorClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();

        // Check if clicking in the top inventory (compactor GUI)
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            // Check if it's a compactor slot
            if (isCompactorSlot(slot)) {
                // Handle compactor slot click
                plugin.getCompactorManager().handleCompactorClick(player, slot, event.getCurrentItem());
                event.setCancelled(true);
                return;
            } else {
                // Cancel interaction with non-compactor slots in the GUI
                event.setCancelled(true);
                return;
            }
        }

        // Check if clicking on player inventory
        if (event.getClickedInventory() != null &&
                event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.PLAYER) {
            // Player clicked on their inventory - try to add item to compactor
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                plugin.getCompactorManager().handleInventoryClick(player, clickedItem);
            }
            event.setCancelled(true);
            return;
        }
    }

    private void updateCraftingResult(Player player, org.bukkit.inventory.Inventory inventory) {
        try {
            // Get items from crafting grid
            ItemStack[] craftingGrid = new ItemStack[9];

            // Get crafting slots from config
            ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
            List<Integer> craftingSlots = guiConfig.getIntegerList("crafting-gui.crafting-slots");

            if (craftingSlots.isEmpty()) {
                // Default crafting slots
                int[] defaultSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
                for (int i = 0; i < 9; i++) {
                    craftingGrid[i] = inventory.getItem(defaultSlots[i]);
                }
            } else {
                // Custom crafting slots
                for (int i = 0; i < Math.min(9, craftingSlots.size()); i++) {
                    craftingGrid[i] = inventory.getItem(craftingSlots.get(i));
                }
            }

            // Check if this matches any custom recipe
            String matchedRecipe = findMatchingRecipe(craftingGrid);
            int resultSlot = guiConfig.getInt("crafting-gui.result-slot", 24);

            if (matchedRecipe != null) {
                CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager().getCustomRecipes().get(matchedRecipe);
                if (recipe != null && (recipe.getPermission() == null || player.hasPermission(recipe.getPermission()))) {
                    // Calculate how many items can be crafted
                    int maxCraftable = calculateMaxCraftable(craftingGrid, recipe);
                    if (maxCraftable > 0) {
                        ItemStack result = recipe.getResult().clone();
                        result.setAmount(maxCraftable);
                        inventory.setItem(resultSlot, result);
                    } else {
                        inventory.setItem(resultSlot, null);
                    }
                } else {
                    inventory.setItem(resultSlot, null);
                }
            } else {
                // Clear result slot if no recipe matches
                inventory.setItem(resultSlot, null);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating crafting result: " + e.getMessage());
            int resultSlot = plugin.getConfigManager().getGuiConfig().getInt("crafting-gui.result-slot", 24);
            inventory.setItem(resultSlot, null);
        }
    }

    private int calculateMaxCraftable(ItemStack[] craftingGrid, CraftingManager.CraftingRecipe recipe) {
        int maxCraftable = Integer.MAX_VALUE;
        Map<Integer, CraftingManager.RecipeIngredient> ingredients = recipe.getIngredients();

        for (Map.Entry<Integer, CraftingManager.RecipeIngredient> entry : ingredients.entrySet()) {
            int slot = entry.getKey();
            CraftingManager.RecipeIngredient ingredient = entry.getValue();
            ItemStack gridItem = craftingGrid[slot - 1];

            if (gridItem == null || gridItem.getType() == Material.AIR) {
                return 0; // Missing ingredient
            }

            int available = gridItem.getAmount();
            int needed = ingredient.getAmount();
            int possibleCrafts = available / needed;

            maxCraftable = Math.min(maxCraftable, possibleCrafts);
        }

        return maxCraftable == Integer.MAX_VALUE ? 0 : maxCraftable;
    }

    private void handleRecipeViewClick(InventoryClickEvent event, Player player) {
        // Cancel all clicks in recipe view
        event.setCancelled(true);
    }

    private void handleRecipeEditorClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();

        // Allow interaction with crafting slots and result slot
        if (isCraftingSlot(slot) || isResultSlot(slot)) {
            // Update result when crafting grid changes
            if (!isResultSlot(slot)) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    updateCraftingResult(player, event.getInventory());
                }, 1L);
            }
            return;
        }

        // Handle control buttons
        if (slot == 36) { // Cancel button
            plugin.getMessageManager().sendMessage(player, "crafting.recipe-cancelled");
            player.closeInventory();
            event.setCancelled(true);
            return;
        }

        if (slot == 44) { // Save button
            String recipeId = event.getView().getTitle().replace("§6Edit Recipe: ", "");
            saveRecipeFromEditor(player, event.getInventory(), recipeId);
            plugin.getMessageManager().sendMessage(player, "crafting.recipe-saved");
            player.closeInventory();
            event.setCancelled(true);
            return;
        }

        // Cancel interaction with other slots
        event.setCancelled(true);
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

    private boolean isCompactorSlot(int slot) {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> compactorSlots = guiConfig.getIntegerList("compactor-gui.compactor-slots");

        if (compactorSlots.isEmpty()) {
            // Default compactor slots
            return slot >= 19 && slot <= 25;
        }

        return compactorSlots.contains(slot);
    }

    private void processCraftingResult(InventoryClickEvent event, Player player) {
        // Get items from crafting grid
        ItemStack[] craftingGrid = new ItemStack[9];

        // Get crafting slots from config
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> craftingSlots = guiConfig.getIntegerList("crafting-gui.crafting-slots");

        if (craftingSlots.isEmpty()) {
            // Default crafting slots
            int[] defaultSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            for (int i = 0; i < 9; i++) {
                craftingGrid[i] = event.getInventory().getItem(defaultSlots[i]);
            }
        } else {
            // Custom crafting slots
            for (int i = 0; i < Math.min(9, craftingSlots.size()); i++) {
                craftingGrid[i] = event.getInventory().getItem(craftingSlots.get(i));
            }
        }

        // Check if this matches any custom recipe
        String matchedRecipe = findMatchingRecipe(craftingGrid);
        if (matchedRecipe != null) {
            // Process custom recipe
            processCustomRecipe(event, player, matchedRecipe, craftingGrid);
        }
        // If no custom recipe matches, allow vanilla crafting to proceed
    }

    private String findMatchingRecipe(ItemStack[] craftingGrid) {
        // Check each custom recipe to see if it matches the current crafting grid
        for (String recipeId : plugin.getCraftingManager().getCustomRecipes().keySet()) {
            if (matchesRecipe(craftingGrid, recipeId)) {
                return recipeId;
            }
        }
        return null;
    }

    private boolean matchesRecipe(ItemStack[] craftingGrid, String recipeId) {
        CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager().getCustomRecipes().get(recipeId);
        if (recipe == null) {
            return false;
        }

        Map<Integer, CraftingManager.RecipeIngredient> ingredients = recipe.getIngredients();

        // Check each slot in the recipe
        for (int slot = 1; slot <= 9; slot++) {
            ItemStack gridItem = craftingGrid[slot - 1];
            CraftingManager.RecipeIngredient requiredIngredient = ingredients.get(slot);

            if (requiredIngredient == null) {
                // No ingredient required in this slot
                if (gridItem != null && gridItem.getType() != Material.AIR) {
                    return false; // But there's an item here
                }
            } else {
                // Ingredient required in this slot
                if (gridItem == null || gridItem.getType() == Material.AIR) {
                    return false; // But slot is empty
                }

                if (!matchesIngredient(gridItem, requiredIngredient)) {
                    return false; // Item doesn't match required ingredient
                }

                if (gridItem.getAmount() < requiredIngredient.getAmount()) {
                    return false; // Not enough items
                }
            }
        }

        return true;
    }

    private boolean matchesIngredient(ItemStack item, CraftingManager.RecipeIngredient ingredient) {
        if (ingredient.isCustom()) {
            // Check if it's a custom ingredient
            return ItemUtils.isCustomItem(item, ingredient.getName());
        } else {
            // Check if it's a vanilla material and NOT a custom item
            try {
                Material material = Material.valueOf(ingredient.getName().toUpperCase());
                return item.getType() == material && !ItemUtils.isCustomItem(item);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    private void processCustomRecipe(InventoryClickEvent event, Player player, String recipeId, ItemStack[] craftingGrid) {
        CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager().getCustomRecipes().get(recipeId);
        if (recipe == null) {
            event.setCancelled(true);
            return;
        }

        // Check permission
        if (recipe.getPermission() != null && !player.hasPermission(recipe.getPermission())) {
            plugin.getMessageManager().sendMessage(player, "crafting.no-permission-craft");
            event.setCancelled(true);
            return;
        }

        // Get the result item from the result slot
        ItemStack resultSlotItem = event.getCurrentItem();
        if (resultSlotItem == null || resultSlotItem.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }

        int craftAmount = resultSlotItem.getAmount();

        // Check if player has inventory space
        ItemStack result = recipe.getResult().clone();
        result.setAmount(craftAmount);
        if (!ItemUtils.hasInventorySpace(player.getInventory(), result)) {
            plugin.getMessageManager().sendMessage(player, "crafting.inventory-full");
            event.setCancelled(true);
            return;
        }

        // Check if player is taking the result item
        ItemStack cursor = event.getCursor();

        if (cursor != null && cursor.getType() != Material.AIR) {
            // Player has item on cursor, check if it can stack
            if (!ItemUtils.isSimilar(cursor, result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                event.setCancelled(true);
                return;
            }
        }

        // Remove ingredients from crafting grid
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> craftingSlots = guiConfig.getIntegerList("crafting-gui.crafting-slots");

        Map<Integer, CraftingManager.RecipeIngredient> ingredients = recipe.getIngredients();
        for (Map.Entry<Integer, CraftingManager.RecipeIngredient> entry : ingredients.entrySet()) {
            int slot = entry.getKey();
            CraftingManager.RecipeIngredient ingredient = entry.getValue();

            ItemStack gridItem = craftingGrid[slot - 1];
            if (gridItem != null) {
                int totalNeeded = ingredient.getAmount() * craftAmount;
                int newAmount = gridItem.getAmount() - totalNeeded;
                if (newAmount <= 0) {
                    int guiSlot = craftingSlots.isEmpty() ? convertSlotToGUI(slot) : craftingSlots.get(slot - 1);
                    event.getInventory().setItem(guiSlot, null);
                } else {
                    gridItem.setAmount(newAmount);
                }
            }
        }

        // Give result to player
        if (cursor != null && cursor.getType() != Material.AIR) {
            cursor.setAmount(cursor.getAmount() + result.getAmount());
        } else {
            event.setCursor(result);
        }

        // Clear result slot
        int resultSlot = guiConfig.getInt("crafting-gui.result-slot", 24);
        event.getInventory().setItem(resultSlot, null);

        // Execute effects if any
        executeItemEffects(player, recipe, "CRAFT");

        event.setCancelled(true);

        // Update crafting result after processing
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateCraftingResult(player, event.getInventory());
        }, 1L);
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

    private void executeItemEffects(Player player, CraftingManager.CraftingRecipe recipe, String action) {
        ConfigurationSection effects = recipe.getEffects();
        if (effects == null) return;

        ConfigurationSection actionSection = effects.getConfigurationSection(action);
        if (actionSection == null) return;

        for (String key : actionSection.getKeys(false)) {
            String command = actionSection.getString(key);
            if (command != null) {
                executeEffectCommand(player, command);
            }
        }
    }

    private void executeEffectCommand(Player player, String command) {
        String finalCommand = command.replace("%player%", player.getName());

        if (finalCommand.startsWith("player: ")) {
            // Execute as player
            String playerCommand = finalCommand.substring(8);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.performCommand(playerCommand);
            });
        } else if (finalCommand.startsWith("console: ")) {
            // Execute as console
            String consoleCommand = finalCommand.substring(9);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), consoleCommand);
            });
        }
    }

    private void saveRecipeFromEditor(Player player, org.bukkit.inventory.Inventory inventory, String recipeId) {
        try {
            ConfigurationSection recipesSection = plugin.getConfigManager().getCraftingConfig().getConfigurationSection("recipes");
            if (recipesSection == null) {
                recipesSection = plugin.getConfigManager().getCraftingConfig().createSection("recipes");
            }

            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeId);
            if (recipeSection == null) {
                recipeSection = recipesSection.createSection(recipeId);
            }

            // Clear existing recipe
            ConfigurationSection oldRecipeSection = recipeSection.getConfigurationSection("recipe");
            if (oldRecipeSection != null) {
                recipeSection.set("recipe", null);
            }

            ConfigurationSection newRecipeSection = recipeSection.createSection("recipe");

            // Save crafting grid items
            int[] guiSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(guiSlots[i]);
                if (item != null && item.getType() != Material.AIR) {
                    int slot = i + 1;
                    String ingredientData = getIngredientData(item);
                    if (ingredientData != null) {
                        newRecipeSection.set(String.valueOf(slot), ingredientData);

                        // Register new custom ingredient if needed
                        registerCustomIngredientIfNeeded(item);
                    }
                }
            }

            // Save result item if present
            ItemStack resultItem = inventory.getItem(24);
            if (resultItem != null && resultItem.getType() != Material.AIR) {
                ItemUtils.saveItemToConfig(resultItem, recipeSection);
            }

            // Save config
            plugin.getConfigManager().saveCraftingConfig();

            // Reload crafting data
            plugin.getCraftingManager().loadCraftingData();

        } catch (Exception e) {
            player.sendMessage("§cError saving recipe: " + e.getMessage());
            plugin.getLogger().severe("Error saving recipe from editor: " + e.getMessage());
        }
    }

    private String getIngredientData(ItemStack item) {
        // Check if it's a custom ingredient
        String customId = ItemUtils.getCustomItemId(item);
        if (customId != null) {
            return customId + ":" + item.getAmount();
        }

        // Check if it's a vanilla material (not custom)
        if (!ItemUtils.isCustomItem(item)) {
            return item.getType().name() + ":" + item.getAmount();
        }

        return null;
    }

    private void registerCustomIngredientIfNeeded(ItemStack item) {
        String customId = ItemUtils.getCustomItemId(item);
        if (customId != null) {
            // Already a registered custom ingredient
            return;
        }

        // Check if it's a vanilla item
        if (!ItemUtils.isCustomItem(item)) {
            return;
        }

        // This is a new custom item, register it
        String newId = "custom_" + System.currentTimeMillis();

        ConfigurationSection ingredientsSection = plugin.getConfigManager().getCraftingConfig().getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            ingredientsSection = plugin.getConfigManager().getCraftingConfig().createSection("ingredients");
        }

        ConfigurationSection newIngredient = ingredientsSection.createSection(newId);
        ItemUtils.saveItemToConfig(item, newIngredient);

        // Add custom data to the item
        ItemUtils.addCustomData(item, "item_id", newId);
    }

    private void returnCraftingItems(Player player, org.bukkit.inventory.Inventory inventory) {
        ConfigurationSection guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> craftingSlots = guiConfig.getIntegerList("crafting-gui.crafting-slots");

        // Get crafting slots to check
        int[] slotsToCheck;
        if (craftingSlots.isEmpty()) {
            slotsToCheck = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
        } else {
            slotsToCheck = craftingSlots.stream().mapToInt(Integer::intValue).toArray();
        }

        // Return items from crafting slots to player
        for (int slot : slotsToCheck) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                // Try to add to player inventory
                if (ItemUtils.hasInventorySpace(player.getInventory(), item)) {
                    player.getInventory().addItem(item);
                } else {
                    // Drop item if inventory is full
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
                inventory.setItem(slot, null);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // Get dynamic titles from config for consistency
        String craftingTitle = ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("crafting-gui.title", "§6Custom Crafting Table"));
        String compactorTitle = ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("compactor-gui.title", "§6Compactor"));

        if (title.equals(craftingTitle)) {
            // Stop crafting scanning
            stopCraftingScanning(player);

            // Return items from crafting slots to player
            returnCraftingItems(player, event.getInventory());


        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        try {
            // Check if it's a custom recipe result with RIGHT_CLICK effects
            String recipeId = ItemUtils.getRecipeId(item);
            if (recipeId != null) {
                CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager().getCustomRecipes().get(recipeId);
                if (recipe != null) {
                    executeItemEffects(player, recipe, "RIGHT_CLICK");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing item effects: " + e.getMessage());
        }
    }
}