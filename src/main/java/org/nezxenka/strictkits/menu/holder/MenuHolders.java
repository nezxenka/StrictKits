package org.nezxenka.strictkits.menu.holder;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@UtilityClass
public class MenuHolders {

    private static final boolean SNAPSHOT_FREE_LOOKUP = supportsSnapshotFreeLookup();

    public static MenuHolder of(Inventory inventory) {
        InventoryHolder holder = SNAPSHOT_FREE_LOOKUP ? inventory.getHolder(false) : inventory.getHolder();
        return holder instanceof MenuHolder menu ? menu : null;
    }

    public static MenuHolder openBy(Player player) {
        return of(player.getOpenInventory().getTopInventory());
    }

    @SuppressWarnings("deprecation")
    public static void close(Player player) {
        player.closeInventory();
        player.updateInventory();
    }

    public static void closeAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (openBy(online) != null) {
                online.closeInventory();
            }
        }
    }

    private static boolean supportsSnapshotFreeLookup() {
        try {
            Inventory.class.getMethod("getHolder", boolean.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
