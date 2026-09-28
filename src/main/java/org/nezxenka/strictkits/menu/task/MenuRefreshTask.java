package org.nezxenka.strictkits.menu.task;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.nezxenka.strictkits.menu.holder.KitListHolder;
import org.nezxenka.strictkits.menu.holder.MenuHolders;
import org.nezxenka.strictkits.menu.view.KitListMenu;

@RequiredArgsConstructor
public final class MenuRefreshTask implements Runnable {

    private final Plugin plugin;
    private final KitListMenu menu;

    private BukkitTask task;

    public void start(int periodTicks) {
        stop();
        if (periodTicks > 0) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this, periodTicks, periodTicks);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (MenuHolders.openBy(online) instanceof KitListHolder holder) {
                menu.refresh(online, holder);
            }
        }
    }
}
