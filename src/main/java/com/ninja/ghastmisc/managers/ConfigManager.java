package com.ninja.ghastmisc.managers;

import com.ninja.ghastmisc.GhastMiscPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {
    private final GhastMiscPlugin plugin;
    private FileConfiguration craftingConfig;
    private FileConfiguration vouchersConfig;
    private FileConfiguration commandsConfig;
    private FileConfiguration guiConfig;
    private File craftingFile;
    private File vouchersFile;
    private File commandsFile;
    private File guiFile;

    public ConfigManager(GhastMiscPlugin plugin) {
        this.plugin = plugin;
        setupConfigFiles();
        loadConfigs();
    }

    private void setupConfigFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        craftingFile = new File(plugin.getDataFolder(), "crafting.yml");
        vouchersFile = new File(plugin.getDataFolder(), "vouchers.yml");
        commandsFile = new File(plugin.getDataFolder(), "commands.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");

        saveDefaultConfig("crafting.yml", craftingFile);
        saveDefaultConfig("vouchers.yml", vouchersFile);
        saveDefaultConfig("commands.yml", commandsFile);
        saveDefaultConfig("gui.yml", guiFile);
    }

    private void saveDefaultConfig(String resourceName, File destination) {
        if (!destination.exists()) {
            try {
                InputStream resource = plugin.getResource(resourceName);
                if (resource != null) {
                    Files.copy(resource, destination.toPath());
                    plugin.getLogger().info("Created default " + resourceName);
                } else {
                    // Create default content if resource doesn't exist
                    createDefaultConfig(resourceName, destination);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create " + resourceName + ": " + e.getMessage());
                createDefaultConfig(resourceName, destination);
            }
        }
    }

    private void createDefaultConfig(String configName, File destination) {
        try {
            if (destination.createNewFile()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(destination);

                switch (configName) {
                    case "crafting.yml":
                        createDefaultCraftingConfig(config);
                        break;
                    case "vouchers.yml":
                        createDefaultVouchersConfig(config);
                        break;
                    case "commands.yml":
                        createDefaultCommandsConfig(config);
                        break;
                    case "gui.yml":
                        createDefaultGuiConfig(config);
                        break;
                }

                config.save(destination);
                plugin.getLogger().info("Created default " + configName + " with basic structure");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default " + configName + ": " + e.getMessage());
        }
    }

    private void createDefaultCraftingConfig(FileConfiguration config) {
        config.createSection("ingredients");
        config.createSection("recipes");

        // Add example ingredient
        config.set("ingredients.example_ingredient.item-name", "&6Example Ingredient");
        config.set("ingredients.example_ingredient.material", "DIAMOND");
        config.set("ingredients.example_ingredient.lore", java.util.Arrays.asList("&7This is an example ingredient"));
        config.set("ingredients.example_ingredient.custom-model-data", 1000);
    }

    private void createDefaultVouchersConfig(FileConfiguration config) {
        // Create default voucher sections
        config.set("levels.voucher-name", "&5Levels&6Voucher");
        config.set("levels.voucher-lore", java.util.Arrays.asList(
                "&4This voucher will give you {multiplier}X Levels boost",
                "&6for {time} minutes",
                "&6Right-click to redeem"
        ));
        config.set("levels.voucher-material", "PAPER");
        config.set("levels.placeholder", "%levels_booster_status%");
        config.set("levels.stackable", false);
        config.set("levels.glow", true);
        config.set("levels.true-action.stop", true);
        config.set("levels.true-action.stop-msg", "&cYou already have a booster active!");
        config.set("levels.false-action.stop", false);
        config.set("levels.false-action.command", "levels booster %player_name% {time} {multiplier}");

        // Money voucher with new none/not-none system
        config.set("money.voucher-name", "&2Money&6Voucher");
        config.set("money.voucher-lore", java.util.Arrays.asList(
                "&4This voucher will give you {multiplier}X Money boost",
                "&6for {time} minutes",
                "&6Right-click to redeem"
        ));
        config.set("money.voucher-material", "PAPER");
        config.set("money.placeholder", "%money_booster_status%");
        config.set("money.stackable", false);
        config.set("money.glow", true);
        config.set("money.none.stop", false);
        config.set("money.none.command", "money booster %player_name% {time} {multiplier}");
        config.set("money.not-none.stop", true);
        config.set("money.not-none.stop-msg", "&cYou already have a money booster active!");
    }

    private void createDefaultCommandsConfig(FileConfiguration config) {
        config.createSection("commands");

        // Add example command replacement
        config.set("commands.example.command", "example ");
        config.set("commands.example.op-only", true);
        config.set("commands.example.result", java.util.Arrays.asList("say Hello %player_name%!"));
    }

    private void createDefaultGuiConfig(FileConfiguration config) {
        // Crafting GUI Configuration
        config.set("crafting-gui.title", "&6Custom Crafting Table");
        config.set("crafting-gui.size", 45);
        config.set("crafting-gui.result-slot", 24);
        config.set("crafting-gui.crafting-slots", java.util.Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30));

        // Background slots
        config.set("crafting-gui.background.0.material", "BLACK_STAINED_GLASS_PANE");
        config.set("crafting-gui.background.0.item-name", " ");
        config.set("crafting-gui.background.1.material", "BLACK_STAINED_GLASS_PANE");
        config.set("crafting-gui.background.1.item-name", " ");

        // Additional decorative items
        config.set("crafting-gui.additional-items.decoration.slot", 22);
        config.set("crafting-gui.additional-items.decoration.material", "PLAYER_HEAD");
        config.set("crafting-gui.additional-items.decoration.texture", "http://textures.minecraft.net/texture/e3fc52264d8ad9e654f415bef01a23947edbccccf649373289bea4d149541f70");
        config.set("crafting-gui.additional-items.decoration.item-name", "&6Crafting Helper");
        config.set("crafting-gui.additional-items.decoration.lore", java.util.Arrays.asList("&7Place items in the grid", "&7to create custom items!"));

        // Compactor GUI Configuration
        config.set("compactor-gui.title", "&6Compactor");
        config.set("compactor-gui.size", 45);
        config.set("compactor-gui.compactor-slots", java.util.Arrays.asList(19, 20, 21, 22, 23, 24, 25));
    }

    public void loadConfigs() {
        // Use async loading for better performance
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    craftingConfig = YamlConfiguration.loadConfiguration(craftingFile);
                    vouchersConfig = YamlConfiguration.loadConfiguration(vouchersFile);
                    commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
                    guiConfig = YamlConfiguration.loadConfiguration(guiFile);

                    // Validate and initialize required sections
                    validateConfigs();

                    // Switch back to main thread for final setup
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.getLogger().info("All configuration files loaded successfully");
                        }
                    }.runTask(plugin);

                } catch (Exception e) {
                    plugin.getLogger().severe("Error loading configurations: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void validateConfigs() {
        // Validate crafting config
        if (!craftingConfig.contains("ingredients")) {
            craftingConfig.createSection("ingredients");
        }
        if (!craftingConfig.contains("recipes")) {
            craftingConfig.createSection("recipes");
        }

        // Validate vouchers config
        for (String section : new String[]{"levels", "essence", "money"}) {
            if (!vouchersConfig.contains(section)) {
                vouchersConfig.createSection(section);
                vouchersConfig.set(section + ".voucher-name", "&7" + section.substring(0, 1).toUpperCase() + section.substring(1) + " Voucher");
                vouchersConfig.set(section + ".voucher-material", "PAPER");
                vouchersConfig.set(section + ".stackable", false);

                if (section.equals("money")) {
                    // Set up none/not-none for money
                    vouchersConfig.set(section + ".none.stop", false);
                    vouchersConfig.set(section + ".none.command", "");
                    vouchersConfig.set(section + ".not-none.stop", false);
                    vouchersConfig.set(section + ".not-none.command", "");
                } else {
                    // Set up true/false for others
                    vouchersConfig.set(section + ".true-action.stop", false);
                    vouchersConfig.set(section + ".true-action.command", "");
                    vouchersConfig.set(section + ".false-action.stop", false);
                    vouchersConfig.set(section + ".false-action.command", "");
                }
            }
        }

        // Validate commands config
        if (!commandsConfig.contains("commands")) {
            commandsConfig.createSection("commands");
        }

        // Validate GUI config
        if (!guiConfig.contains("crafting-gui")) {
            guiConfig.createSection("crafting-gui");
            guiConfig.set("crafting-gui.title", "&6Custom Crafting Table");
            guiConfig.set("crafting-gui.size", 45);
        }
    }

    public void reloadConfigs() {
        loadConfigs();

        // Reload all managers after config reload
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getCraftingManager() != null) {
                    plugin.getCraftingManager().loadCraftingData();
                }
                if (plugin.getVoucherManager() != null) {
                    plugin.getVoucherManager().loadVoucherData();
                }
                if (plugin.getCommandManager() != null) {
                    plugin.getCommandManager().loadCommandData();
                }
                if (plugin.getCompactorManager() != null) {
                    plugin.getCompactorManager().loadCompactorData();
                }
            }
        }.runTaskLater(plugin, 20L); // Wait 1 second for async loading
    }

    // Save methods with error handling
    public void saveCraftingConfig() {
        try {
            craftingConfig.save(craftingFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crafting config: " + e.getMessage());
        }
    }

    public void saveVouchersConfig() {
        try {
            vouchersConfig.save(vouchersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save vouchers config: " + e.getMessage());
        }
    }

    public void saveCommandsConfig() {
        try {
            commandsConfig.save(commandsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save commands config: " + e.getMessage());
        }
    }

    public void saveGuiConfig() {
        try {
            guiConfig.save(guiFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save GUI config: " + e.getMessage());
        }
    }

    // Getters with null safety
    public FileConfiguration getCraftingConfig() {
        if (craftingConfig == null) {
            craftingConfig = YamlConfiguration.loadConfiguration(craftingFile);
        }
        return craftingConfig;
    }

    public FileConfiguration getVouchersConfig() {
        if (vouchersConfig == null) {
            vouchersConfig = YamlConfiguration.loadConfiguration(vouchersFile);
        }
        return vouchersConfig;
    }

    public FileConfiguration getCommandsConfig() {
        if (commandsConfig == null) {
            commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
        }
        return commandsConfig;
    }

    public FileConfiguration getGuiConfig() {
        if (guiConfig == null) {
            guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        }
        return guiConfig;
    }
}