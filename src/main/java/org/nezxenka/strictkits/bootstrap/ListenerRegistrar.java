package org.nezxenka.strictkits.bootstrap;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
public final class ListenerRegistrar {

    private final Plugin plugin;

    public void register(Listener... listeners) {
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}
