package org.nezxenka.StrictKits.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.StrictKits.config.Lang;
import org.nezxenka.StrictKits.config.Settings;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitManager;
import org.nezxenka.StrictKits.kit.KitService;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.util.GUItems;
import org.nezxenka.StrictKits.util.TimeFormat;

import java.util.ArrayList;
import java.util.List;

public final class KitMenu {

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_EXIT = 49;
    public static final int SLOT_NEXT = 53;

    private final KitManager kits;
    private final KitService service;
    private final PlayerDataManager players;
    private final Lang lang;
    private final Settings settings;

    public KitMenu(KitManager kits, KitService service, PlayerDataManager players, Lang lang, Settings settings) {
        this.kits = kits;
        this.service = service;
        this.players = players;
        this.lang = lang;
        this.settings = settings;
    }

    public boolean open(Player player, int requestedPage) {
        List<Kit> visible = visibleKits(player);
        if (visible.isEmpty()) {
            player.sendMessage(kits.size() == 0 ? lang.getNoKitServer() : lang.getNoAccess());
            return false;
        }

        int rows = settings.getGuiRows();
        int capacity = Math.max(9, (rows - 1) * 9);
        int totalPages = (visible.size() + capacity - 1) / capacity;
        int page = Math.min(Math.max(1, requestedPage), totalPages);

        int size = totalPages > 1 ? rows * 9 : sizeFor(visible.size());

        Kit[] slots = new Kit[size];
        MenuHolder holder = MenuHolder.list(page, totalPages, slots);
        Inventory inventory = Bukkit.createInventory(holder, size, lang.getGuiTitle(page, totalPages));
        holder.setInventory(inventory);

        PlayerData data = players.get(player.getUniqueId());
        int offset = (page - 1) * capacity;
        int placed = 0;
        int limit = totalPages > 1 ? capacity : size;
        for (int i = offset; i < visible.size() && placed < limit; i++, placed++) {
            Kit kit = visible.get(i);
            slots[placed] = kit;
            inventory.setItem(placed, decorate(player, data, kit));
        }

        if (totalPages > 1) {
            if (page > 1) {
                inventory.setItem(SLOT_PREVIOUS, GUItems.getPreviousButton());
            }
            if (page < totalPages) {
                inventory.setItem(SLOT_NEXT, GUItems.getNextButton());
            }
            inventory.setItem(SLOT_EXIT, GUItems.getExitButton());
        } else if (size >= 54) {
            inventory.setItem(SLOT_EXIT, GUItems.getExitButton());
        }

        player.openInventory(inventory);
        return true;
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
        for (int i = 0; i < all.size(); i++) {
            Kit kit = all.get(i);
            if (kit.getIcon() == null) {
                continue;
            }
            if (showAll || kit.hasAccess(player)) {
                visible.add(kit);
            }
        }
        return visible;
    }

    private ItemStack decorate(Player player, PlayerData data, Kit kit) {
        ItemStack item = kit.getIcon().clone();
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

    private String statusLine(Player player, PlayerData data, Kit kit) {
        if (data == null) {
            return lang.getLoreAvailable();
        }
        if (!kit.hasAccess(player)) {
            return lang.getLoreNoPermission();
        }
        if (kit.isOneTimeUse()) {
            return data.hasClaim(kit.getKey()) ? lang.getLoreClaimed() : lang.getLoreAvailable();
        }
        long remaining = service.remainingCooldown(data, kit);
        return remaining > 0L
                ? lang.getLoreCooldown(TimeFormat.getFormattedCooldown(remaining))
                : lang.getLoreAvailable();
    }
}
