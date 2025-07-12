package com.ninja.ghastmisc;

import com.ninja.ghastmisc.commands.GhastMiscCommand;
import com.ninja.ghastmisc.listeners.*;
import com.ninja.ghastmisc.managers.*;
import com.ninja.ghastmisc.utils.ItemUtils;
import com.ninja.ghastmisc.utils.ColorUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class GhastMiscPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private CraftingManager craftingManager;
    private VoucherManager voucherManager;
    private CommandManager commandManager;
    private AutoCraftManager autoCraftManager;
    private CompactorManager compactorManager;
    private InventoryLockListener inventoryLockListener;

    @Override
    public void onEnable() {
        getLogger().info("Initializing GhastMisc v" + getDescription().getVersion());

        // Initialize utilities with plugin reference
        ItemUtils.initialize(this);
        ColorUtils.initialize(this);

        // 1. First load essential configurations
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);

        // 2. Then initialize dependent managers
        this.craftingManager = new CraftingManager(this);
        this.voucherManager = new VoucherManager(this);
        this.commandManager = new CommandManager(this);
        this.autoCraftManager = new AutoCraftManager(this);
        this.compactorManager = new CompactorManager(this);

        // 3. Register commands and events
        registerCommands();
        registerListeners();

        // 4. Start background tasks
        autoCraftManager.start();

        getLogger().info("Successfully enabled GhastMisc!");
    }

    private void registerCommands() {
        GhastMiscCommand mainCommand = new GhastMiscCommand(this);
        getCommand("gm").setExecutor(mainCommand);
        getCommand("gm").setTabCompleter(mainCommand);

        // Register dynamic commands from commands.yml
        commandManager.registerDynamicCommands();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new VoucherListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandInterceptListener(this), this);

        // Store reference to inventory lock listener
        this.inventoryLockListener = new InventoryLockListener(this);
        getServer().getPluginManager().registerEvents(inventoryLockListener, this);
    }

    @Override
    public void onDisable() {
        if (autoCraftManager != null) {
            autoCraftManager.stop();
        }
        getLogger().info("GhastMisc has been disabled");
    }

    public void reload() {
        // Reload all configurations
        configManager.reloadConfigs();
        messageManager.reloadMessages();

        // Reload all managers
        craftingManager.loadCraftingData();
        voucherManager.loadVoucherData();
        commandManager.loadCommandData();
        compactorManager.loadCompactorData();

        // Re-register dynamic commands
        commandManager.registerDynamicCommands();

        getLogger().info("Plugin reloaded successfully");
    }

    // Getters
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public CraftingManager getCraftingManager() { return craftingManager; }
    public VoucherManager getVoucherManager() { return voucherManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public AutoCraftManager getAutoCraftManager() { return autoCraftManager; }
    public CompactorManager getCompactorManager() { return compactorManager; }
    public InventoryLockListener getInventoryLockListener() { return inventoryLockListener; }
}