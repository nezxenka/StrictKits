package org.nezxenka.strictkits.kit.model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.nezxenka.strictkits.util.item.ItemStacks;

import java.util.Arrays;

public final class KitContents {

    public static final int STORAGE_SIZE = 36;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SLOT = 40;
    public static final KitContents EMPTY = new KitContents(new ItemStack[0], new ItemStack[0]);

    private final ItemStack[] main;
    private final ItemStack[] armor;

    private KitContents(ItemStack[] main, ItemStack[] armor) {
        this.main = main;
        this.armor = armor;
    }

    public static KitContents of(ItemStack[] main, ItemStack[] armor) {
        ItemStack[] mainCopy = ItemStacks.copyAll(main);
        ItemStack[] armorCopy = ItemStacks.copyAll(armor);
        return mainCopy.length == 0 && armorCopy.length == 0 ? EMPTY : new KitContents(mainCopy, armorCopy);
    }

    public static KitContents capture(PlayerInventory inventory) {
        ItemStack[] main = inventory.getContents();
        int armorEnd = Math.min(main.length, STORAGE_SIZE + ARMOR_SIZE);
        if (armorEnd > STORAGE_SIZE) {
            Arrays.fill(main, STORAGE_SIZE, armorEnd, null);
        }
        return of(main, inventory.getArmorContents());
    }

    public int mainSize() {
        return main.length;
    }

    public int armorSize() {
        return armor.length;
    }

    public ItemStack mainItem(int slot) {
        return ItemStacks.copy(main[slot]);
    }

    public ItemStack armorItem(int index) {
        return ItemStacks.copy(armor[index]);
    }

    public ItemStack[] mainItems() {
        return ItemStacks.copyAll(main);
    }

    public ItemStack[] armorItems() {
        return ItemStacks.copyAll(armor);
    }

    public boolean isEmpty() {
        return Arrays.stream(main).allMatch(ItemStacks::isAir) && Arrays.stream(armor).allMatch(ItemStacks::isAir);
    }
}
