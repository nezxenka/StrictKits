package org.nezxenka.StrictKits.util;

import org.bukkit.inventory.ItemStack;

public final class ItemSerializer {

    private static final ItemStack[] EMPTY = new ItemStack[0];

    private ItemSerializer() {
    }

    public static ItemStack[] copy(ItemStack[] source) {
        if (source == null || source.length == 0) {
            return EMPTY;
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }

    public static ItemStack[] empty() {
        return EMPTY;
    }
}
