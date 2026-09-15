package org.nezxenka.StrictKits.gui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.nezxenka.StrictKits.kit.Kit;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class MenuHolder implements InventoryHolder {

    public enum Type {
        KIT_LIST,
        PREVIEW
    }

    public static final int NO_SLOT = -1;

    private final Type type;
    private final int page;
    private final int totalPages;
    @Getter(AccessLevel.NONE)
    private final Kit[] slots;
    private final int previousSlot;
    private final int exitSlot;
    private final int nextSlot;
    @Setter
    private Inventory inventory;

    public static MenuHolder list(int page, int totalPages, Kit[] slots, int previousSlot, int exitSlot, int nextSlot) {
        return new MenuHolder(Type.KIT_LIST, page, totalPages, slots, previousSlot, exitSlot, nextSlot);
    }

    public static MenuHolder preview(int exitSlot) {
        return new MenuHolder(Type.PREVIEW, 1, 1, new Kit[0], NO_SLOT, exitSlot, NO_SLOT);
    }

    public Kit kitAt(int slot) {
        return slot >= 0 && slot < slots.length ? slots[slot] : null;
    }
}
