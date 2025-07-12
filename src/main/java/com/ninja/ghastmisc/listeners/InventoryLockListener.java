package com.ninja.ghastmisc.listeners;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryLockListener implements Listener {

    private final GhastMiscPlugin plugin;
    private final Map<UUID, Boolean> lockedInventories = new HashMap<>();

    public InventoryLockListener(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // Only lock inventory if player is in voucher confirmation GUI AND inventory is locked
        if (isLockedInventory(playerId) && isVoucherConfirmationGUI(event.getView().getTitle())
                && event.getClickedInventory() != null) {
            if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // Only lock inventory if player is in voucher confirmation GUI AND inventory is locked
        if (isLockedInventory(playerId) && isVoucherConfirmationGUI(event.getView().getTitle())) {
            // Check if dragging involves player inventory
            for (int slot : event.getRawSlots()) {
                if (slot >= event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isVoucherConfirmationGUI(String title) {
        // Get dynamic title from config
        String voucherConfirmTitle = "§6Confirm Voucher Use"; // This could also be made configurable
        return voucherConfirmTitle.equals(title);
    }

    public void lockInventory(UUID playerId) {
        lockedInventories.put(playerId, true);
    }

    public void unlockInventory(UUID playerId) {
        lockedInventories.remove(playerId);
    }

    public boolean isLockedInventory(UUID playerId) {
        return lockedInventories.getOrDefault(playerId, false);
    }
}