package org.nezxenka.strictkits.kit.delivery;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.model.KitContents;
import org.nezxenka.strictkits.util.item.ItemStacks;

public final class KitDelivery {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    @SuppressWarnings("deprecation")
    public void deliver(Player player, Kit kit) {
        KitContents contents = kit.getContents();
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < contents.mainSize(); slot++) {
            ItemStack item = contents.mainItem(slot);
            if (ItemStacks.isAir(item)) {
                continue;
            }
            if (slot == KitContents.OFFHAND_SLOT && ItemStacks.isAir(inventory.getItemInOffHand())) {
                inventory.setItemInOffHand(item);
            } else {
                give(player, item);
            }
        }
        equipArmor(player, contents);
        player.updateInventory();
    }

    private static void equipArmor(Player player, KitContents contents) {
        PlayerInventory inventory = player.getInventory();
        for (int index = Math.min(contents.armorSize(), ARMOR_SLOTS.length) - 1; index >= 0; index--) {
            ItemStack item = contents.armorItem(index);
            if (ItemStacks.isAir(item)) {
                continue;
            }
            EquipmentSlot slot = ARMOR_SLOTS[index];
            if (ItemStacks.isAir(inventory.getItem(slot))) {
                inventory.setItem(slot, item);
            } else {
                give(player, item);
            }
        }
    }

    private static void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
