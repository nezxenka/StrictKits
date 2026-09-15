package org.nezxenka.StrictKits.listeners;

import lombok.RequiredArgsConstructor;
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
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.gui.MenuHolder;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.player.PlayerDataManager;

import java.util.function.Consumer;

@RequiredArgsConstructor
public final class Listeners implements Listener {

    private final Main plugin;

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.getPlayers().preload(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager players = plugin.getPlayers();
        if (players.isLoaded(player.getUniqueId())) {
            players.markOnline(player.getUniqueId());
            giveFirstJoinKits(player);
            return;
        }
        players.getWorkers().execute(() -> {
            players.markOnline(player.getUniqueId());
            runForOnline(player, this::giveFirstJoinKits);
        });
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
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder menu)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()
                || slot < 0) {
            return;
        }

        if (slot == menu.getExitSlot()) {
            runForOnline(player, this::closeMenu);
            return;
        }
        if (menu.getType() == MenuHolder.Type.PREVIEW) {
            return;
        }
        if (slot == menu.getPreviousSlot() && menu.getPage() > 1) {
            reopen(player, menu.getPage() - 1);
            return;
        }
        if (slot == menu.getNextSlot() && menu.getPage() < menu.getTotalPages()) {
            reopen(player, menu.getPage() + 1);
            return;
        }

        Kit kit = menu.kitAt(slot);
        if (kit == null) {
            return;
        }
        if (event.isRightClick() && plugin.getSettings().isGuiPreview()) {
            runForOnline(player, online -> plugin.getKitMenu().preview(online, kit));
            return;
        }
        if (plugin.getKitService().give(player, kit)) {
            reopen(player, menu.getPage());
        }
    }

    private void giveFirstJoinKits(Player player) {
        if (!player.hasPlayedBefore()) {
            plugin.getKitService().giveFirstJoinKits(player);
        }
    }

    @SuppressWarnings("deprecation")
    private void closeMenu(Player player) {
        player.closeInventory();
        player.updateInventory();
    }

    private void reopen(Player player, int page) {
        runForOnline(player, online -> plugin.getKitMenu().open(online, page));
    }

    private void runForOnline(Player player, Consumer<Player> action) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                action.accept(player);
            }
        });
    }
}
