package org.nezxenka.strictkits.listener.player;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nezxenka.strictkits.kit.claim.ClaimService;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;
import org.nezxenka.strictkits.util.scheduler.PlayerTasks;

import java.util.UUID;

@RequiredArgsConstructor
public final class PlayerConnectionListener implements Listener {

    private final PlayerDataManager players;
    private final ClaimService claims;
    private final PlayerTasks tasks;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            players.load(event.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        players.markOnline(uuid);
        if (players.isLoaded(uuid)) {
            giveFirstJoinKits(player);
        } else {
            players.requestLoad(uuid, () -> tasks.runLater(player, this::giveFirstJoinKits));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        players.markOffline(event.getPlayer().getUniqueId());
    }

    private void giveFirstJoinKits(Player player) {
        if (!player.hasPlayedBefore()) {
            claims.giveFirstJoinKits(player);
        }
    }
}
