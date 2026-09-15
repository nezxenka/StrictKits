package org.nezxenka.StrictKits.gui;

import lombok.RequiredArgsConstructor;
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
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.TimeFormat;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class KitMenu {

    private static final int ROW_SIZE = 9;
    private static final int MAX_ROWS = 6;
    private static final int MIN_PAGED_ROWS = 2;
    private static final int PREVIEW_SIZE = 54;
    private static final int PREVIEW_BOOTS_SLOT = 39;
    private static final int PREVIEW_EXIT_SLOT = 49;

    private final KitManager kits;
    private final KitService service;
    private final PlayerDataManager players;
    private final Messages messages;
    private final Settings settings;
    private final MenuItems items;

    public void open(Player player, int requestedPage) {
        List<Kit> visible = kits.all().stream()
                .filter(kit -> settings.isDisplayWithoutPermission() || kit.hasAccess(player))
                .toList();
        if (visible.isEmpty()) {
            Messenger.send(player, kits.size() == 0 ? messages.getNoKitsOnServer() : messages.getNoAccess());
            return;
        }

        int rows = settings.getGuiRows();
        int capacity = Math.max(ROW_SIZE, (rows - 1) * ROW_SIZE);
        int totalPages = (visible.size() + capacity - 1) / capacity;
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        boolean paged = totalPages > 1;
        int size = paged ? Math.max(MIN_PAGED_ROWS, rows) * ROW_SIZE : rowsFor(visible.size()) * ROW_SIZE;

        Kit[] slots = new Kit[size];
        MenuHolder holder = paged
                ? MenuHolder.list(page, totalPages, slots, size - ROW_SIZE, size - ROW_SIZE + ROW_SIZE / 2, size - 1)
                : MenuHolder.list(page, totalPages, slots, MenuHolder.NO_SLOT, MenuHolder.NO_SLOT, MenuHolder.NO_SLOT);
        Inventory inventory = Bukkit.createInventory(holder, size, messages.getGuiTitle().format(page, totalPages));
        holder.setInventory(inventory);

        PlayerData data = players.get(player.getUniqueId());
        int offset = (page - 1) * capacity;
        int count = Math.min(capacity, visible.size() - offset);
        for (int slot = 0; slot < count; slot++) {
            Kit kit = visible.get(offset + slot);
            slots[slot] = kit;
            inventory.setItem(slot, decorate(player, data, kit));
        }

        if (paged) {
            if (page > 1) {
                inventory.setItem(holder.getPreviousSlot(), items.getPreviousButton());
            }
            if (page < totalPages) {
                inventory.setItem(holder.getNextSlot(), items.getNextButton());
            }
            inventory.setItem(holder.getExitSlot(), items.getExitButton());
        }

        player.openInventory(inventory);
    }

    public void preview(Player player, Kit kit) {
        if (settings.isPreviewRequiresPermission() && !player.hasPermission("strictkits.preview")) {
            Messenger.send(player, messages.getNoPermission());
            return;
        }
        if (kit.isEmpty()) {
            Messenger.send(player, messages.getKitEmpty());
            return;
        }
        MenuHolder holder = MenuHolder.preview(PREVIEW_EXIT_SLOT);
        Inventory inventory = Bukkit.createInventory(holder, PREVIEW_SIZE, messages.getGuiPreviewTitle().format(kit.getName()));
        holder.setInventory(inventory);

        ItemStack[] main = kit.getMainContent();
        for (int slot = 0; slot < Math.min(main.length, Kit.STORAGE_SIZE); slot++) {
            inventory.setItem(slot, main[slot]);
        }
        ItemStack[] armor = kit.getArmorContent();
        for (int index = 0; index < Math.min(armor.length, Kit.ARMOR_SIZE); index++) {
            inventory.setItem(PREVIEW_BOOTS_SLOT - index, armor[index]);
        }
        if (main.length > Kit.OFFHAND_SLOT) {
            inventory.setItem(Kit.OFFHAND_SLOT, main[Kit.OFFHAND_SLOT]);
        }
        inventory.setItem(PREVIEW_EXIT_SLOT, items.getExitButton());
        player.openInventory(inventory);
    }

    public void refresh(Player player, MenuHolder holder, Inventory inventory) {
        if (holder.getType() != MenuHolder.Type.KIT_LIST) {
            return;
        }
        PlayerData data = players.get(player.getUniqueId());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            Kit kit = holder.kitAt(slot);
            if (kit != null) {
                inventory.setItem(slot, decorate(player, data, kit));
            }
        }
    }

    private static int rowsFor(int count) {
        return Math.min(MAX_ROWS, Math.max(1, (count + ROW_SIZE - 1) / ROW_SIZE));
    }

    private ItemStack decorate(Player player, PlayerData data, Kit kit) {
        ItemStack icon = kit.getIcon();
        ItemStack item = icon != null
                ? icon.clone()
                : items.createDefaultIcon(messages.getDefaultIconName().format(kit.getName()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>(1);
        lore.add(statusLine(player, data, kit));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
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
                ? messages.getLoreCooldown().format(TimeFormat.format(remaining))
                : messages.getLoreAvailable();
    }
}
