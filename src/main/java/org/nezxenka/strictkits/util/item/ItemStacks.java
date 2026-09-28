package org.nezxenka.strictkits.util.item;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@UtilityClass
public class ItemStacks {

    public static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    public static ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public static ItemStack[] copyAll(ItemStack[] items) {
        if (items == null || items.length == 0) {
            return new ItemStack[0];
        }
        return Arrays.stream(items).map(ItemStacks::copy).toArray(ItemStack[]::new);
    }
}
