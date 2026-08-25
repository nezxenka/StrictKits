package org.nezxenka.StrictKits.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.nezxenka.StrictKits.kit.Kit;

public final class MenuHolder implements InventoryHolder {

    public enum Type {
        KIT_LIST,
        PREVIEW
    }

    private final Type type;
    private final Kit kit;
    private final int page;
    private final int totalPages;
    private final Kit[] slots;
    private Inventory inventory;

    private MenuHolder(Type type, Kit kit, int page, int totalPages, Kit[] slots) {
        this.type = type;
        this.kit = kit;
        this.page = page;
        this.totalPages = totalPages;
        this.slots = slots;
    }

    public static MenuHolder list(int page, int totalPages, Kit[] slots) {
        return new MenuHolder(Type.KIT_LIST, null, page, totalPages, slots);
    }

    public static MenuHolder preview(Kit kit) {
        return new MenuHolder(Type.PREVIEW, kit, 1, 1, null);
    }

    public Type getType() {
        return type;
    }

    public Kit getKit() {
        return kit;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public Kit kitAt(int slot) {
        if (slots == null || slot < 0 || slot >= slots.length) {
            return null;
        }
        return slots[slot];
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
