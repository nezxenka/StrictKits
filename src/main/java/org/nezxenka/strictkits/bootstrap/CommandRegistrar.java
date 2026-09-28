package org.nezxenka.strictkits.bootstrap;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class CommandRegistrar {

    private final JavaPlugin plugin;

    public void register(String name, TabExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Команда " + name + " не объявлена в plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
