# 🎮 GhastMisc Plugin

**A comprehensive Minecraft plugin featuring advanced crafting systems, voucher management, command replacements, and auto-crafting capabilities.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.19+-brightgreen.svg)](https://minecraft.net)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)


---

## 📋 Table of Contents

- [Features](#-features)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [GUI Customization](#-gui-customization)
- [Crafting System](#-crafting-system)
- [Voucher System](#-voucher-system)
- [Command Replacement](#-command-replacement)
- [Auto-Crafting & Compactor](#-auto-crafting--compactor)
- [PlaceholderAPI Integration](#-placeholderapi-integration)


---

## ✨ Features

### 🔧 **Core Systems**
- **Custom Crafting System** - Create complex custom recipes with permissions
- **Voucher Management** - Advanced voucher system with PlaceholderAPI integration
- **Command Replacement** - Replace and redirect commands dynamically
- **Auto-Crafting** - Automatic crafting for registered players
- **OneCraft System** - Craft items in hand once with smart inventory management
- **Compactor System** - Advanced item compacting with JSON persistence

### 🎨 **GUI Features**
- **Fully Customizable GUIs** - Configure size, titles, slots, and decorations
- **Dynamic Title Loading** - GUI titles loaded from configuration files
- **Smart Inventory Management** - Prevents overfilling and handles space efficiently
- **Click-to-Add System** - Intuitive item management in compactor
- **Visual Feedback** - Color-coded glass panes and decorative elements

### 🔒 **Security & Performance**
- **Permission-Based Access** - Granular permission system for all features
- **Async Configuration Loading** - Non-blocking config operations
- **JSON Data Persistence** - Reliable data storage for compactor settings
- **Error Handling** - Comprehensive error catching and logging
- **Memory Optimization** - Efficient data structures and cleanup

---

## 📦 Installation

### Prerequisites
- **Minecraft Server**: 1.19+ (Paper/Spigot)
- **Java**: 17 or higher
- **PlaceholderAPI**: Optional but recommended for voucher placeholders

### Steps
1. **Download** the latest `GhastMisc.jar` from releases
2. **Place** the jar file in your server's `plugins/` folder
3. **Restart** your server
4. **Configure** the plugin using the generated config files
5. **Reload** the plugin with `/gm reload`

### File Structure
```
plugins/
├── GhastMisc.jar
└── GhastMisc/
    ├── config.yml
    ├── crafting.yml
    ├── vouchers.yml
    ├── commands.yml
    ├── gui.yml
    ├── messages.yml
    └── compactor.json
```

---

## ⚙️ Configuration

### 📁 **Configuration Files**

#### **crafting.yml** - Custom Recipes & Ingredients
```yaml
ingredients:
  magic_crystal:
    item-name: "&5Magic Crystal"
    material: "DIAMOND"
    lore:
      - "&7A mystical crystal with"
      - "&7powerful magical properties"
    custom-model-data: 1001
    glow: true

recipes:
  magic_sword:
    itemname: "&6Magic Sword"
    material: "DIAMOND_SWORD"
    lore:
      - "&7A sword infused with magic"
    permission: "ghastmisc.craft.magic"
    glow: true
    recipe:
      1: "magic_crystal:1"
      2: "magic_crystal:1"
      4: "STICK:1"
      7: "STICK:1"
```

#### **gui.yml** - GUI Customization
```yaml
crafting-gui:
  title: "&6✨ Magical Crafting Table"
  size: 45
  result-slot: 24
  crafting-slots: [10, 11, 12, 19, 20, 21, 28, 29, 30]
  
  background:
    0:
      material: "BLACK_STAINED_GLASS_PANE"
      item-name: " "
  
  additional-items:
    decoration:
      slot: 22
      material: "PLAYER_HEAD"
      texture: "http://textures.minecraft.net/texture/e3fc52264d8ad9e654f415bef01a23947edbccccf649373289bea4d149541f70"
      item-name: "&6Crafting Helper"
      lore:
        - "&7Place items in the grid"
        - "&7to create magical items!"

compactor-gui:
  title: "&6🔧 Item Compactor"
  size: 45
  compactor-slots: [19, 20, 21, 22, 23, 24, 25]
```

#### **vouchers.yml** - Voucher Configuration
```yaml
levels:
  voucher-name: "&5⭐ Levels Booster"
  voucher-lore:
    - "&7This voucher grants you"
    - "&6{multiplier}x &7levels boost"
    - "&7for &6{time} &7minutes"
    - ""
    - "&eRight-click to activate!"
  voucher-material: "PAPER"
  placeholder: "%levels_booster_status%"
  stackable: false
  glow: true
  
  true-action:
    stop: true
    stop-msg: "&cYou already have an active booster!"
  
  false-action:
    stop: false
    command: "levels booster %player_name% {time} {multiplier}"
```

#### **commands.yml** - Command Replacements
```yaml
commands:
  heal_command:
    command: "heal "
    op-only: false
    permission: "ghastmisc.heal"
    result:
      - "heal %player_name%"
      - "tellraw %player_name% {\"text\":\"You have been healed!\",\"color\":\"green\"}"
    tab-complete:
      - "@a"
```

---

## 🎮 Commands

### **Main Commands**
| Command      | Description               | Permission        |
|--------------|---------------------------|-------------------|
| `/gm help`   | Show help menu            | `ghastmisc.use`   |
| `/gm reload` | Reload all configurations | `ghastmisc.admin` |

### **Crafting Commands**
| Command                                 | Description                | Permission        |
|-----------------------------------------|----------------------------|-------------------|
| `/gm craft gui`                         | Open custom crafting table | `ghastmisc.craft` |
| `/gm craft list <recipes\|ingredients>` | List custom items          | `ghastmisc.admin` |
| `/gm craft register <id>`               | Register item in hand      | `ghastmisc.admin` |
| `/gm craft give <id> [player] [amount]` | Give custom item           | `ghastmisc.admin` |
| `/gm craft take <id> [player] [amount]` | Take custom item           | `ghastmisc.admin` |
| `/gm craft view <id>`                   | View recipe details        | `ghastmisc.admin` |
| `/gm craft editor <id>`                 | Edit recipe in GUI         | `ghastmisc.admin` |

### **Auto-Crafting Commands**
| Command         | Description          | Permission            |
|-----------------|----------------------|-----------------------|
| `/gm autocraft` | Toggle auto-crafting | `ghastmisc.autocraft` |
| `/gm onecraft`  | Craft held item once | `ghastmisc.onecraft`  |
| `/gm compactor` | Open compactor GUI   | `ghastmisc.compactor` |

### **Voucher Commands**
| Command                                           | Description  | Permission        |
|---------------------------------------------------|--------------|-------------------|
| `/gm voucher <type> <time> <multiplier> <player>` | Give voucher | `ghastmisc.admin` |

### **Command Management**
| Command                          | Description               | Permission        |
|----------------------------------|---------------------------|-------------------|
| `/gm commands list`              | List command replacements | `ghastmisc.admin` |
| `/gm command debug <id>`         | Debug command by ID       | `ghastmisc.admin` |
| `/gm command test <id> [player]` | Test command execution    | `ghastmisc.admin` |

---

## 🔐 Permissions

### **Permission Nodes**
```yaml
ghastmisc.*:                    # All permissions
ghastmisc.use:                  # Basic plugin access (default: true)
ghastmisc.admin:                # Admin commands (default: op)
ghastmisc.craft:                # Crafting GUI access (default: true)
ghastmisc.autocraft:            # Auto-crafting toggle (default: true)
ghastmisc.onecraft:             # OneCraft command (default: true)
ghastmisc.compactor:            # Compactor access (default: true)
ghastmisc.voucher:              # Voucher usage (default: true)
```

### **Custom Recipe Permissions**
Add `permission: "your.custom.permission"` to any recipe in `crafting.yml` to restrict access.

---

## 🎨 GUI Customization

### **Customizable Elements**

#### **Crafting GUI**
- **Title**: Custom colored title with formatting codes
- **Size**: Any valid inventory size (9, 18, 27, 36, 45, 54)
- **Crafting Slots**: Define custom 3x3 grid positions
- **Result Slot**: Position for crafted item display
- **Background**: Custom glass panes and decorative items
- **Additional Items**: Player heads, books, decorative elements

#### **Compactor GUI**
- **Title**: Custom title with color codes
- **Size**: Configurable inventory size (recommended: 45)
- **Compactor Slots**: 7 centered slots for item selection
- **Background**: Color-coded glass panes for visual appeal
- **Help Items**: Informational items with instructions

### **Advanced Customization**

#### **Player Head Textures**
```yaml
additional-items:
  custom_head:
    slot: 22
    material: "PLAYER_HEAD"
    texture: "http://textures.minecraft.net/texture/YOUR_TEXTURE_URL"
    item-name: "&6Custom Head"
    lore:
      - "&7This is a custom player head"
      - "&7with a unique texture!"
```

#### **Dynamic Backgrounds**
```yaml
background:
  0: { material: "BLACK_STAINED_GLASS_PANE", item-name: " " }
  1: { material: "GRAY_STAINED_GLASS_PANE", item-name: " " }
  # ... continue for all non-functional slots
```

---

## 🔨 Crafting System

### **Creating Custom Recipes**

#### **Step 1: Define Ingredients**
```yaml
ingredients:
  enchanted_stick:
    item-name: "&5Enchanted Stick"
    material: "STICK"
    lore:
      - "&7A stick imbued with magic"
    custom-model-data: 2001
    glow: true
```

#### **Step 2: Create Recipe**
```yaml
recipes:
  magic_wand:
    itemname: "&6Magic Wand"
    material: "STICK"
    lore:
      - "&7A powerful magical wand"
      - "&7Right-click to cast spells!"
    permission: "ghastmisc.craft.wand"
    glow: true
    recipe:
      1: "DIAMOND:1"           # Top-left
      2: "enchanted_stick:1"   # Top-center
      3: "DIAMOND:1"           # Top-right
      5: "enchanted_stick:1"   # Center
      8: "enchanted_stick:1"   # Bottom-center
    effects:
      CRAFT:
        1: "console: tellraw %player% {\"text\":\"You crafted a Magic Wand!\",\"color\":\"gold\"}"
      RIGHT_CLICK:
        1: "player: particle flame ~ ~1 ~ 1 1 1 0.1 50"
```

### **Recipe Slot Numbers**
```
1 | 2 | 3
4 | 5 | 6
7 | 8 | 9
```

### **Effect Types**
- **CRAFT**: Executed when item is crafted
- **RIGHT_CLICK**: Executed when item is right-clicked
- **LEFT_CLICK**: Executed when item is left-clicked

### **Command Prefixes**
- **console:**: Execute as console
- **player:**: Execute as player

---

## 🎫 Voucher System

### **Voucher Types**
- **levels**: Experience/level boosters
- **essence**: Custom essence boosters
- **money**: Economy boosters

### **PlaceholderAPI Integration**

#### **Placeholder Checking**
The plugin checks placeholder values to determine voucher behavior:
```yaml
placeholder: "%levels_booster_status%"
true-action:
  stop: true
  stop-msg: "&cYou already have an active booster!"
false-action:
  stop: false
  command: "levels booster %player_name% {time} {multiplier}"
```

#### **Supported Placeholders**
- `%player_name%`: Player's username
- `{time}`: Voucher duration
- `{multiplier}`: Boost multiplier
- Any PlaceholderAPI placeholder

### **Creating Custom Vouchers**

#### **Example: Money Booster**
```yaml
money:
  voucher-name: "&2💰 Money Booster"
  voucher-lore:
    - "&7Increases money gain by"
    - "&6{multiplier}x &7for &6{time} &7minutes"
    - ""
    - "&aRight-click to activate!"
  voucher-material: "EMERALD"
  placeholder: "%economy_booster_active%"
  stackable: true
  glow: true
  custom-model-data: 3001
  
  true-action:
    stop: true
    stop-msg: "&cYou already have a money booster active!"
  
  false-action:
    stop: false
    command: "economy boost %player_name% {multiplier} {time}"
```

### **Voucher Usage**
1. **Give Voucher**: `/gm voucher money 30 2.0 PlayerName`
2. **Player Right-Clicks**: Voucher in inventory
3. **Confirmation GUI**: Appears with confirm/cancel options
4. **Execution**: Command runs if confirmed

---

## 🔄 Command Replacement

### **How It Works**
The plugin intercepts commands and replaces them with custom actions.

### **Configuration Example**
```yaml
commands:
  teleport_home:
    command: "home "
    op-only: false
    permission: "ghastmisc.home"
    result:
      - "tp %player_name% 0 100 0"
      - "tellraw %player_name% {\"text\":\"Welcome home!\",\"color\":\"green\"}"
      - "playsound minecraft:entity.enderman.teleport player %player_name%"
    tab-complete:
      - "spawn"
      - "base"
      - "shop"
```

### **Advanced Features**
- **Permission Checking**: Restrict command access
- **OP-Only Commands**: Limit to operators
- **Tab Completion**: Custom tab completion options
- **Multiple Actions**: Execute multiple commands per replacement
- **Placeholder Support**: Use PlaceholderAPI placeholders

### **Command Testing**
```bash
/gm command debug teleport_home    # View command details
/gm command test teleport_home PlayerName    # Test execution
```

---

## 🤖 Auto-Crafting & Compactor

### **Auto-Crafting System**

#### **How It Works**
1. **Toggle**: `/gm autocraft` enables/disables for player
2. **Background Task**: Runs every 3 seconds (60 ticks)
3. **Smart Crafting**: Only crafts what fits in inventory
4. **Permission Checking**: Respects recipe permissions

#### **Features**
- **Inventory Space Checking**: Prevents item loss
- **Ingredient Detection**: Automatically finds required items
- **Batch Crafting**: Crafts maximum possible amount
- **Permission Respect**: Only crafts allowed recipes

### **OneCraft System**

#### **Usage**
1. **Hold Item**: Hold the item you want to craft
2. **Execute**: `/gm onecraft`
3. **Smart Crafting**: Crafts once if possible


### **Compactor System**

#### **Features**
- **45-Slot GUI**: Spacious interface
- **7 Centered Slots**: Slots 19-25 for item selection
- **Click-to-Add**: Click inventory items to add to compactor
- **Click-to-Remove**: Click compactor items to remove
- **JSON Persistence**: Saves settings to `compactor.json`
- **Auto-Processing**: Crafts items when GUI closes

#### **Data Structure**
```json
{
  "player-uuid": {
    "0": "item_id_1",
    "1": "item_id_2",
    "2": "vanilla_material_name"
  }
}
```

#### **Usage Flow**
1. **Open**: `/gm compactor`
2. **Add Items**: Click items in your inventory
3. **Remove Items**: Click items in compactor slots
4. **Auto-Craft**: Close GUI to process all items

---

## 🔗 PlaceholderAPI Integration

### **Installation**
1. Install PlaceholderAPI plugin
2. Download required expansions
3. Restart server
4. Configure voucher placeholders

### **Voucher Placeholder Checking**

#### **Debug Information**
The plugin logs placeholder parsing for debugging:
```
[INFO] Placeholder check for PlayerName: %levels_booster_status% -> true -> true
[INFO] Original: %levels_booster_status% | Parsed: true | Result: true
```

#### **Common Issues**
- **Placeholder Not Found**: Install required expansion
- **Wrong Format**: Use exact placeholder syntax
- **Case Sensitivity**: Placeholders are case-sensitive

### **Supported Expansions**
- **Player**: `%player_name%`, `%player_uuid%`
- **Server**: `%server_name%`, `%server_online%`
- **Custom**: Any installed PlaceholderAPI expansion

---

## 🛠️ Developer API

### **Events**
```java
// Custom crafting event
@EventHandler
public void onCustomCraft(CustomCraftEvent event) {
    Player player = event.getPlayer();
    String recipeId = event.getRecipeId();
    ItemStack result = event.getResult();
    
    // Custom logic here
}

// Voucher use event
@EventHandler
public void onVoucherUse(VoucherUseEvent event) {
    Player player = event.getPlayer();
    String voucherType = event.getVoucherType();
    
    // Custom logic here
}
```

### **API Methods**
```java
// Get plugin instance
GhastMiscPlugin plugin = (GhastMiscPlugin) Bukkit.getPluginManager().getPlugin("GhastMisc");

// Access managers
CraftingManager crafting = plugin.getCraftingManager();
VoucherManager vouchers = plugin.getVoucherManager();
AutoCraftManager autocraft = plugin.getAutoCraftManager();

// Check if item is custom
boolean isCustom = ItemUtils.isCustomItem(itemStack);
String itemId = ItemUtils.getCustomItemId(itemStack);

// Create custom items
ItemStack customItem = ItemUtils.createCustomItem(configSection);
```

### **Creating Addons**
```java
public class MyAddon extends JavaPlugin {
    private GhastMiscPlugin ghastMisc;
    
    @Override
    public void onEnable() {
        ghastMisc = (GhastMiscPlugin) getServer().getPluginManager().getPlugin("GhastMisc");
        
        if (ghastMisc == null) {
            getLogger().severe("GhastMisc not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register custom recipes
        registerCustomRecipes();
    }
    
    private void registerCustomRecipes() {
        // Add custom recipes programmatically
    }
}
```

---

## 🐛 Troubleshooting

### **Common Issues**

#### **Plugin Not Loading**
```
[ERROR] Could not load 'plugins/GhastMisc.jar'
```
**Solutions:**
- Check Java version (requires 17+)
- Verify Minecraft version compatibility
- Check for conflicting plugins

#### **Config Errors**
```
[ERROR] Error loading configurations
```
**Solutions:**
- Validate YAML syntax
- Check file permissions
- Restore from backup

#### **GUI Not Opening**
```
Player cannot open crafting GUI
```
**Solutions:**
- Check permissions: `ghastmisc.craft`
- Verify GUI configuration
- Check for inventory conflicts

#### **Recipes Not Working**
```
Custom recipes not crafting
```
**Solutions:**
- Verify ingredient registration
- Check recipe permissions
- Validate recipe syntax

#### **PlaceholderAPI Issues**
```
Placeholders not parsing
```
**Solutions:**
- Install PlaceholderAPI
- Download required expansions
- Check placeholder syntax

### **Debug Commands**
```bash
/gm craft list recipes          # List all recipes
/gm craft list ingredients      # List all ingredients
/gm command debug <id>          # Debug command replacement
/gm reload                      # Reload configurations
```

### **Log Analysis**
```
[INFO] Loaded 5 ingredients and 3 recipes
[INFO] Registered 2 dynamic commands
[INFO] All configuration files loaded successfully
```

### **Performance Optimization**
- **Async Loading**: Configs load asynchronously
- **Efficient Caching**: Recipes cached in memory
- **Smart Updates**: Only update when necessary
- **Cleanup**: Automatic cleanup of unused data


---

**Made with ❤️ by the Ninja0_0**