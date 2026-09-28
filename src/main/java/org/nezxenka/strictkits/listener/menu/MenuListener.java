package org.nezxenka.strictkits.listener.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.nezxenka.strictkits.menu.holder.KitListHolder;
import org.nezxenka.strictkits.menu.holder.MenuHolder;
import org.nezxenka.strictkits.menu.holder.MenuHolders;
import org.nezxenka.strictkits.util.scheduler.PlayerTasks;

@RequiredArgsConstructor
public final class MenuListener implements Listener {

    private final KitListClickHandler listClicks;
    private final PlayerTasks tasks;

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (MenuHolders.of(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        MenuHolder holder = MenuHolders.of(event.getView().getTopInventory());
        if (holder == null) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()
                || slot < 0) {
            return;
        }
        if (slot == holder.getExitSlot()) {
            tasks.runLater(player, MenuHolders::close);
            return;
        }
        if (holder instanceof KitListHolder list) {
            listClicks.handle(player, list, slot, event.isRightClick());
        }
    }
}
