package org.nezxenka.strictkits.bootstrap;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.nezxenka.strictkits.command.admin.AdminCommand;
import org.nezxenka.strictkits.command.player.KitCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.claim.ClaimFeedback;
import org.nezxenka.strictkits.kit.claim.ClaimService;
import org.nezxenka.strictkits.kit.delivery.KitDelivery;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.kit.storage.KitFileStorage;
import org.nezxenka.strictkits.listener.menu.KitListClickHandler;
import org.nezxenka.strictkits.listener.menu.MenuListener;
import org.nezxenka.strictkits.listener.player.PlayerConnectionListener;
import org.nezxenka.strictkits.menu.render.KitIconRenderer;
import org.nezxenka.strictkits.menu.task.MenuRefreshTask;
import org.nezxenka.strictkits.menu.view.KitListMenu;
import org.nezxenka.strictkits.menu.view.KitPreviewMenu;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;
import org.nezxenka.strictkits.storage.api.StorageProvider;
import org.nezxenka.strictkits.storage.factory.StorageFactory;
import org.nezxenka.strictkits.util.scheduler.PlayerTasks;

import java.io.File;

@Getter
public final class PluginContext {

    private static final String KITS_FOLDER = "Kits";

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final StorageProvider storage;
    private final PlayerDataManager players;
    private final KitRegistry kits;
    private final PlayerTasks tasks;
    private final ClaimService claims;
    private final ClaimFeedback feedback;
    private final KitListMenu listMenu;
    private final KitPreviewMenu previewMenu;
    private final MenuRefreshTask menuRefresh;
    private final PluginReloader reloader;

    private PluginContext(JavaPlugin plugin, ConfigManager configs, StorageProvider storage) {
        this.plugin = plugin;
        this.configs = configs;
        this.storage = storage;
        this.players = new PlayerDataManager(storage, plugin.getLogger());
        this.kits = new KitRegistry(new KitFileStorage(new File(plugin.getDataFolder(), KITS_FOLDER), plugin.getLogger()),
                plugin.getLogger());
        this.tasks = new PlayerTasks(plugin);
        this.claims = new ClaimService(kits, players, new KitDelivery());
        this.feedback = new ClaimFeedback(configs);
        this.listMenu = new KitListMenu(configs, kits, players, new KitIconRenderer(configs));
        this.previewMenu = new KitPreviewMenu(configs);
        this.menuRefresh = new MenuRefreshTask(plugin, listMenu);
        this.reloader = new PluginReloader(configs, kits, menuRefresh, plugin.getLogger());
    }

    public static PluginContext create(JavaPlugin plugin) throws StartupException {
        ConfigManager configs = new ConfigManager(plugin);
        configs.load();
        StorageProvider storage = StorageFactory.create(configs.getDatabase(), plugin.getDataFolder(), plugin.getLogger());
        try {
            storage.initialize();
        } catch (Exception e) {
            storage.shutdown();
            throw new StartupException("Не удалось подключиться к " + configs.getDatabase().getStorageType(), e);
        }
        return new PluginContext(plugin, configs, storage);
    }

    public void start() {
        players.start();
        reloader.loadKits();
        menuRefresh.start(configs.getSettings().getMenu().getRefreshTicks());
        registerCommands();
        registerListeners();
        new StartupLoader(plugin.getDataFolder(), configs.getDatabase(), storage, players, plugin.getLogger()).run();
    }

    public void shutdown() {
        menuRefresh.stop();
        kits.shutdown();
        players.shutdown();
        storage.shutdown();
    }

    private void registerCommands() {
        CommandRegistrar registrar = new CommandRegistrar(plugin);
        registrar.register("strictkits", new AdminCommand(configs, kits, players, storage, reloader));
        registrar.register("kit", new KitCommand(configs, kits, players, claims, feedback, listMenu, previewMenu));
    }

    private void registerListeners() {
        KitListClickHandler listClicks = new KitListClickHandler(configs, kits, claims, feedback, listMenu, previewMenu, tasks);
        new ListenerRegistrar(plugin).register(
                new PlayerConnectionListener(players, claims, tasks),
                new MenuListener(listClicks, tasks));
    }
}
