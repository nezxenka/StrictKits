package org.nezxenka.strictkits.kit.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.nezxenka.strictkits.kit.naming.KitNames;
import org.nezxenka.strictkits.permission.Permissions;
import org.nezxenka.strictkits.util.item.ItemStacks;

import java.util.concurrent.TimeUnit;

@Getter
public final class Kit {

    public static final long MAX_COOLDOWN_SECONDS = TimeUnit.DAYS.toSeconds(3650L);

    private final String name;
    private final String key;

    private volatile long cooldown;
    @Setter
    private volatile String permission;
    @Setter
    private volatile boolean oneTimeUse;
    @Setter
    private volatile boolean firstTimeJoinKit;
    @Setter
    @NonNull
    private volatile KitContents contents = KitContents.EMPTY;
    @Getter(AccessLevel.NONE)
    private volatile ItemStack icon;

    public Kit(String name) {
        this.name = name;
        this.key = KitNames.key(name);
        this.permission = Permissions.kit(name);
    }

    public long getCooldownMillis() {
        return TimeUnit.SECONDS.toMillis(cooldown);
    }

    public void setCooldown(long cooldown) {
        this.cooldown = Math.min(Math.max(0L, cooldown), MAX_COOLDOWN_SECONDS);
    }

    public boolean hasIcon() {
        return icon != null;
    }

    public ItemStack getIcon() {
        return ItemStacks.copy(icon);
    }

    public void setIcon(ItemStack icon) {
        this.icon = ItemStacks.copy(icon);
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean hasAccess(Permissible permissible) {
        return permissible.hasPermission(permission)
                || permissible.hasPermission(Permissions.ALL_KITS)
                || permissible.hasPermission(Permissions.ADMIN);
    }
}
