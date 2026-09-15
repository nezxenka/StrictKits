package org.nezxenka.StrictKits.kit;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Getter
public final class Kit {

    public static final long MAX_COOLDOWN_SECONDS = TimeUnit.DAYS.toSeconds(3650L);
    public static final int MAX_NAME_LENGTH = 32;
    public static final int STORAGE_SIZE = 36;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SLOT = 40;
    public static final String PERMISSION_PREFIX = "strictkits.kits.";

    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{Nd}_-]{1," + MAX_NAME_LENGTH + "}");
    private static final ItemStack[] EMPTY = new ItemStack[0];
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    private final String name;
    private final String key;
    @Getter(AccessLevel.NONE)
    private final AtomicBoolean dirty = new AtomicBoolean();

    private volatile long cooldown;
    private volatile String permission;
    private volatile boolean oneTimeUse;
    private volatile boolean firstTimeJoinKit;
    private volatile ItemStack[] mainContent = EMPTY;
    private volatile ItemStack[] armorContent = EMPTY;
    private volatile ItemStack icon;

    public Kit(String name) {
        this.name = name;
        this.key = name.toLowerCase();
        this.permission = PERMISSION_PREFIX + name;
    }

    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    public long getCooldownMillis() {
        return TimeUnit.SECONDS.toMillis(cooldown);
    }

    public void setCooldown(long cooldown) {
        this.cooldown = Math.min(Math.max(0L, cooldown), MAX_COOLDOWN_SECONDS);
        markDirty();
    }

    public void setPermission(String permission) {
        this.permission = permission;
        markDirty();
    }

    public void setOneTimeUse(boolean oneTimeUse) {
        this.oneTimeUse = oneTimeUse;
        markDirty();
    }

    public void setFirstTimeJoinKit(boolean firstTimeJoinKit) {
        this.firstTimeJoinKit = firstTimeJoinKit;
        markDirty();
    }

    public void setMainContent(ItemStack[] mainContent) {
        this.mainContent = copyOf(mainContent);
        markDirty();
    }

    public void setArmorContent(ItemStack[] armorContent) {
        this.armorContent = copyOf(armorContent);
        markDirty();
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon == null ? null : icon.clone();
        markDirty();
    }

    public boolean isEmpty() {
        return Arrays.stream(mainContent).allMatch(Kit::isAir) && Arrays.stream(armorContent).allMatch(Kit::isAir);
    }

    public void markDirty() {
        dirty.set(true);
    }

    public boolean consumeDirty() {
        return dirty.compareAndSet(true, false);
    }

    public boolean hasAccess(Player player) {
        return player.hasPermission(permission)
                || player.hasPermission(PERMISSION_PREFIX + "*")
                || player.hasPermission("strictkits.admin");
    }

    @SuppressWarnings("deprecation")
    public void applyTo(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] main = mainContent;
        for (int slot = 0; slot < main.length; slot++) {
            ItemStack item = main[slot];
            if (isAir(item)) {
                continue;
            }
            if (slot == OFFHAND_SLOT && isAir(inventory.getItemInOffHand())) {
                inventory.setItemInOffHand(item.clone());
            } else {
                deliver(player, item.clone());
            }
        }
        equipArmor(player);
        player.updateInventory();
    }

    private void equipArmor(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armor = armorContent;
        for (int index = Math.min(armor.length, ARMOR_SLOTS.length) - 1; index >= 0; index--) {
            ItemStack item = armor[index];
            if (isAir(item)) {
                continue;
            }
            EquipmentSlot slot = ARMOR_SLOTS[index];
            if (isAir(inventory.getItem(slot))) {
                inventory.setItem(slot, item.clone());
            } else {
                deliver(player, item.clone());
            }
        }
    }

    private static void deliver(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private static ItemStack[] copyOf(ItemStack[] source) {
        if (source == null || source.length == 0) {
            return EMPTY;
        }
        return Arrays.stream(source).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }
}
