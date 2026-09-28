package org.nezxenka.strictkits.menu.holder;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class MenuHolder implements InventoryHolder {

    @Getter
    @Setter
    private Inventory inventory;

    public abstract int getExitSlot();
}
