package org.nezxenka.strictkits;

import org.bukkit.plugin.java.JavaPlugin;
import org.nezxenka.strictkits.bootstrap.PluginContext;
import org.nezxenka.strictkits.bootstrap.StartupException;

import java.util.logging.Level;

public final class StrictKits extends JavaPlugin {

    private PluginContext context;

    @Override
    public void onEnable() {
        try {
            context = PluginContext.create(this);
        } catch (StartupException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e.getCause());
            getLogger().severe("Хранилище не инициализировано, плагин отключается");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        context.start();
        getLogger().info("StrictKits " + getDescription().getVersion() + " включен");
        getLogger().info("Хранилище: " + context.getStorage().name() + ", китов: " + context.getKits().size());
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.shutdown();
            context = null;
        }
        getLogger().info("StrictKits выключен");
    }
}
