package org.nezxenka.StrictKits.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.nezxenka.StrictKits.util.ItemSerializer;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Kit {

    private final String name;
    private final String key;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private volatile long cooldown;
    private volatile String permission;
    private volatile boolean oneTimeUse;
    private volatile boolean firstTimeJoinKit;
    private volatile ItemStack[] mainContent = ItemSerializer.empty();
    private volatile ItemStack[] armorContent = ItemSerializer.empty();
    private volatile ItemStack icon;

    public Kit(String name) {
        this.name = name;
        this.key = name.toLowerCase();
        this.permission = "strictkits.kits." + name;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public long getCooldown() {
        return cooldown;
    }

    public long getCooldownMillis() {
        return cooldown * 1000L;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
        markDirty();
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
        markDirty();
    }

    public boolean isOneTimeUse() {
        return oneTimeUse;
    }

    public void setOneTimeUse(boolean oneTimeUse) {
        this.oneTimeUse = oneTimeUse;
        markDirty();
    }

    public boolean isFirstTimeJoinKit() {
        return firstTimeJoinKit;
    }

    public void setFirstTimeJoinKit(boolean firstTimeJoinKit) {
        this.firstTimeJoinKit = firstTimeJoinKit;
        markDirty();
    }

    public ItemStack[] getMainContent() {
        return mainContent;
    }

    public void setMainContent(ItemStack[] mainContent) {
        this.mainContent = ItemSerializer.copy(mainContent);
        markDirty();
    }

    public ItemStack[] getArmorContent() {
        return armorContent;
    }

    public void setArmorContent(ItemStack[] armorContent) {
        this.armorContent = ItemSerializer.copy(armorContent);
        markDirty();
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon == null ? null : icon.clone();
        markDirty();
    }

    public boolean isEmpty() {
        return mainContent.length == 0 && armorContent.length == 0;
    }

    public void markDirty() {
        dirty.set(true);
    }

    public boolean consumeDirty() {
        return dirty.compareAndSet(true, false);
    }

    public boolean hasAccess(Player player) {
        return player.hasPermission(permission)
                || player.hasPermission("strictkits.kits.*")
                || player.hasPermission("strictkits.admin");
    }

    public void applyTo(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] main = mainContent;
        for (int i = 0; i < main.length; i++) {
            ItemStack item = main[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            ItemStack copy = item.clone();
            if (i == 40 && inventory.getItemInOffHand().getType() == Material.AIR) {
                inventory.setItemInOffHand(copy);
            } else {
                deliver(player, copy);
            }
        }
        ItemStack[] armor = armorContent;
        applyArmor(player, armor, 3);
        applyArmor(player, armor, 2);
        applyArmor(player, armor, 1);
        applyArmor(player, armor, 0);
        player.updateInventory();
    }

    private void applyArmor(Player player, ItemStack[] armor, int slot) {
        if (slot >= armor.length) {
            return;
        }
        ItemStack item = armor[slot];
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemStack copy = item.clone();
        PlayerInventory inventory = player.getInventory();
        ItemStack current;
        switch (slot) {
            case 3:
                current = inventory.getHelmet();
                break;
            case 2:
                current = inventory.getChestplate();
                break;
            case 1:
                current = inventory.getLeggings();
                break;
            default:
                current = inventory.getBoots();
                break;
        }
        if (current == null || current.getType() == Material.AIR) {
            switch (slot) {
                case 3:
                    inventory.setHelmet(copy);
                    break;
                case 2:
                    inventory.setChestplate(copy);
                    break;
                case 1:
                    inventory.setLeggings(copy);
                    break;
                default:
                    inventory.setBoots(copy);
                    break;
            }
            return;
        }
        deliver(player, copy);
    }

    private void deliver(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}
