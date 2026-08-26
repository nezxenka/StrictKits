package org.nezxenka.StrictKits.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.config.Settings;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitManager;
import org.nezxenka.StrictKits.kit.KitService;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.util.GUItems;
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.TimeFormat;

import java.util.ArrayList;
import java.util.List;

public final class KitMenu {

    private static final int MIN_PAGED_ROWS = 2;

    private final KitManager kits;
    private final KitService service;
    private final PlayerDataManager players;
    private final Messages messages;
    private final Settings settings;

    public KitMenu(KitManager kits, KitService service, PlayerDataManager players, Messages messages, Settings settings) {
        this.kits = kits;
        this.service = service;
        this.players = players;
        this.messages = messages;
        this.settings = settings;
    }

    public boolean open(Player player, int requestedPage) {
        List<Kit> visible = visibleKits(player);
        if (visible.isEmpty()) {
            Messenger.send(player, kits.size() == 0 ? messages.getNoKitsOnServer() : messages.getNoAccess());
            return false;
        }

        int configRows = settings.getGuiRows();
        int capacity = Math.max(9, (configRows - 1) * 9);
        int totalPages = (visible.size() + capacity - 1) / capacity;
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        boolean paged = totalPages > 1;

        int size = paged ? Math.max(MIN_PAGED_ROWS, configRows) * 9 : sizeFor(visible.size());
        int previousSlot = paged ? size - 9 : -1;
        int exitSlot = paged ? size - 5 : -1;
        int nextSlot = paged ? size - 1 : -1;

        Kit[] slots = new Kit[size];
        MenuHolder holder = MenuHolder.list(page, totalPages, slots, previousSlot, exitSlot, nextSlot);
        Inventory inventory = Bukkit.createInventory(holder, size, messages.getGuiTitle(page, totalPages));
        holder.setInventory(inventory);

        PlayerData data = players.get(player.getUniqueId());
        int offset = (page - 1) * capacity;
        int limit = paged ? capacity : size;
        int placed = 0;
        for (int i = offset; i < visible.size() && placed < limit; i++, placed++) {
            Kit kit = visible.get(i);
            slots[placed] = kit;
            inventory.setItem(placed, decorate(player, data, kit));
        }

        if (paged) {
            if (page > 1) {
                inventory.setItem(previousSlot, GUItems.getPreviousButton());
            }
            if (page < totalPages) {
                inventory.setItem(nextSlot, GUItems.getNextButton());
            }
            inventory.setItem(exitSlot, GUItems.getExitButton());
        }

        player.openInventory(inventory);
        return true;
    }

    public void refresh(Player player, MenuHolder holder, Inventory inventory) {
        if (holder.getType() != MenuHolder.Type.KIT_LIST) {
            return;
        }
        PlayerData data = players.get(player.getUniqueId());
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            Kit kit = holder.kitAt(slot);
            if (kit != null) {
                inventory.setItem(slot, decorate(player, data, kit));
            }
        }
    }

    private static int sizeFor(int count) {
        int rows = (count + 8) / 9;
        if (rows < 1) {
            rows = 1;
        }
        if (rows > 6) {
            rows = 6;
        }
        return rows * 9;
    }

    private List<Kit> visibleKits(Player player) {
        List<Kit> all = kits.all();
        List<Kit> visible = new ArrayList<>(all.size());
        boolean showAll = settings.isDisplayWithoutPermission();
        for (Kit kit : all) {
            if (showAll || kit.hasAccess(player)) {
                visible.add(kit);
            }
        }
        return visible;
    }

    private ItemStack decorate(Player player, PlayerData data, Kit kit) {
        ItemStack item = iconFor(kit);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.getLore() == null ? new ArrayList<>(1) : new ArrayList<>(meta.getLore());
        lore.add(statusLine(player, data, kit));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack iconFor(Kit kit) {
        ItemStack icon = kit.getIcon();
        if (icon != null) {
            return icon.clone();
        }
        ItemStack fallback = GUItems.getDefaultKitIcon().clone();
        ItemMeta meta = fallback.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.getDefaultIconName(kit.getName()));
            fallback.setItemMeta(meta);
        }
        return fallback;
    }

    private String statusLine(Player player, PlayerData data, Kit kit) {
        if (data == null) {
            return messages.getLoreAvailable();
        }
        if (!kit.hasAccess(player)) {
            return messages.getLoreNoPermission();
        }
        if (kit.isOneTimeUse()) {
            return data.hasClaim(kit.getKey()) ? messages.getLoreClaimed() : messages.getLoreAvailable();
        }
        long remaining = service.remainingCooldown(data, kit);
        return remaining > 0L
                ? messages.getLoreCooldown(TimeFormat.getFormattedCooldown(remaining))
                : messages.getLoreAvailable();
    }
}
