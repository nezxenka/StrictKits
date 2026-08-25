package org.nezxenka.StrictKits.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.gui.KitMenu;
import org.nezxenka.StrictKits.gui.MenuHolder;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitService;

public final class Listeners implements Listener {

    private final Main plugin;

    public Listeners(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        plugin.getPlayers().loadBlocking(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getPlayers().isLoaded(player.getUniqueId())) {
            handleFirstJoin(player);
            return;
        }
        plugin.getPlayers().getWorkers().execute(() -> {
            plugin.getPlayers().loadBlocking(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    handleFirstJoin(player);
                }
            });
        });
    }

    private void handleFirstJoin(Player player) {
        if (!player.hasPlayedBefore()) {
            plugin.getKitService().giveFirstJoinKits(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayers().handleQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        MenuHolder menu = (MenuHolder) holder;
        int slot = event.getRawSlot();

        if (menu.getType() == MenuHolder.Type.PREVIEW) {
            if (slot == KitMenu.SLOT_EXIT) {
                player.closeInventory();
            }
            return;
        }

        if (menu.getTotalPages() > 1) {
            if (slot == KitMenu.SLOT_EXIT) {
                player.closeInventory();
                return;
            }
            if (slot == KitMenu.SLOT_PREVIOUS && menu.getPage() > 1) {
                reopen(player, menu.getPage() - 1);
                return;
            }
            if (slot == KitMenu.SLOT_NEXT && menu.getPage() < menu.getTotalPages()) {
                reopen(player, menu.getPage() + 1);
                return;
            }
        } else if (slot == KitMenu.SLOT_EXIT && event.getView().getTopInventory().getSize() >= 54
                && menu.kitAt(slot) == null) {
            player.closeInventory();
            return;
        }

        Kit kit = menu.kitAt(slot);
        if (kit == null) {
            return;
        }

        KitService service = plugin.getKitService();
        if (event.isRightClick() && plugin.getSettings().isGuiPreview()) {
            Bukkit.getScheduler().runTask(plugin, () -> service.preview(player, kit));
            return;
        }
        if (service.give(player, kit)) {
            reopen(player, menu.getPage());
        }
    }

    private void reopen(Player player, int page) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.getKitMenu().open(player, page);
            }
        });
    }
}
