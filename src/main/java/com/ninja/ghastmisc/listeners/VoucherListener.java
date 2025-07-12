package com.ninja.ghastmisc.listeners;

import com.ninja.ghastmisc.GhastMiscPlugin;
import com.ninja.ghastmisc.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class VoucherListener implements Listener {

    private final GhastMiscPlugin plugin;

    public VoucherListener(GhastMiscPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Check if the item is a voucher
        if (plugin.getVoucherManager().isVoucher(item)) {
            event.setCancelled(true);

            // Handle voucher use
            plugin.getVoucherManager().handleVoucherUse(player, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        // Prevent vouchers from being placed as blocks
        if (plugin.getVoucherManager().isVoucher(item)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(event.getPlayer(), "voucher.cannot-place");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        // Prevent vouchers from being dropped (optional security measure)
        if (plugin.getVoucherManager().isVoucher(item)) {
            // Allow dropping but add a warning
            plugin.getMessageManager().sendMessage(event.getPlayer(), "voucher.dropped-warning");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("§6Confirm Voucher Use")) {
            handleVoucherConfirmation(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        // Prevent dragging in voucher confirmation GUI
        if (title.equals("§6Confirm Voucher Use")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();

        // Prevent vouchers from being moved by hoppers or other automation
        if (plugin.getVoucherManager().isVoucher(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title.equals("§6Confirm Voucher Use")) {
            // Unlock player inventory when closing voucher confirmation
            plugin.getInventoryLockListener().unlockInventory(player.getUniqueId());
        }
    }

    private void handleVoucherConfirmation(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (slot == 20 && clickedItem.getType() == Material.LIME_CONCRETE) {
            // Confirm button clicked
            ItemStack voucher = event.getInventory().getItem(22);
            if (voucher != null && plugin.getVoucherManager().isVoucher(voucher)) {
                plugin.getVoucherManager().confirmVoucherUse(player, voucher);
            } else {
                plugin.getMessageManager().sendMessage(player, "voucher.invalid");
                player.closeInventory();
            }
        } else if (slot == 24 && clickedItem.getType() == Material.RED_CONCRETE) {
            // Cancel button clicked
            plugin.getVoucherManager().cancelVoucherUse(player);
        }
    }
}