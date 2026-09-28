package org.nezxenka.strictkits.util.scheduler;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

@RequiredArgsConstructor
public final class PlayerTasks {

    private final Plugin plugin;

    public void runLater(Player player, Consumer<Player> action) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                action.accept(player);
            }
        });
    }
}
