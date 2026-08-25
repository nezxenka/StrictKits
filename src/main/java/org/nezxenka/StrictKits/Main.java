package org.nezxenka.StrictKits;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nezxenka.StrictKits.commands.AdminCommands;
import org.nezxenka.StrictKits.commands.PlayerCommands;
import org.nezxenka.StrictKits.config.Lang;
import org.nezxenka.StrictKits.config.Settings;
import org.nezxenka.StrictKits.gui.KitMenu;
import org.nezxenka.StrictKits.kit.KitManager;
import org.nezxenka.StrictKits.kit.KitService;
import org.nezxenka.StrictKits.kit.KitStorage;
import org.nezxenka.StrictKits.listeners.Listeners;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.storage.DatabaseConfig;
import org.nezxenka.StrictKits.storage.LegacyImporter;
import org.nezxenka.StrictKits.storage.StorageProvider;
import org.nezxenka.StrictKits.storage.cache.CacheProvider;
import org.nezxenka.StrictKits.storage.cache.MemoryCache;
import org.nezxenka.StrictKits.storage.cache.RedisCache;
import org.nezxenka.StrictKits.storage.sql.MySqlStorage;
import org.nezxenka.StrictKits.storage.sql.SqliteStorage;
import org.nezxenka.StrictKits.util.GUItems;
import org.nezxenka.StrictKits.util.Messenger;

import java.io.File;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    private static Main instance;

    private Settings settings;
    private Lang lang;
    private DatabaseConfig databaseConfig;
    private StorageProvider storage;
    private CacheProvider cache;
    private PlayerDataManager players;
    private KitManager kits;
    private KitService kitService;
    private KitMenu kitMenu;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResourceIfMissing("database.yml");

        loadConfiguration();
        Messenger.detect();

        if (!setupStorage()) {
            getLogger().severe("Хранилище не инициализировано, плагин отключается");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.players = new PlayerDataManager(storage, cache, databaseConfig, getLogger());
        this.players.start();

        this.kits = new KitManager(new KitStorage(new File(getDataFolder(), "Kits"), getLogger()), players.getWorkers());
        int loaded = kits.loadAll();

        this.kitService = new KitService(kits, players, lang, settings);
        this.kitMenu = new KitMenu(kits, kitService, players, lang, settings);

        registerCommands();
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        runLegacyImport();
        preloadOnlinePlayers();

        getLogger().info("StrictKits " + getDescription().getVersion() + " включен");
        getLogger().info("Хранилище: " + storage.name() + ", кэш: " + cache.name() + ", китов: " + loaded);
    }

    @Override
    public void onDisable() {
        if (kits != null) {
            kits.flushAllBlocking();
        }
        if (players != null) {
            players.shutdown();
        }
        if (cache != null) {
            cache.shutdown();
        }
        if (storage != null) {
            storage.shutdown();
        }
        instance = null;
        getLogger().info("StrictKits выключен");
    }

    private void loadConfiguration() {
        FileConfiguration config = getConfig();
        this.settings = new Settings(config);
        this.lang = new Lang(config);
        GUItems.load(config);
        this.databaseConfig = new DatabaseConfig(loadDatabaseConfig());
    }

    private FileConfiguration loadDatabaseConfig() {
        File file = new File(getDataFolder(), "database.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        java.io.InputStream defaults = getResource("database.yml");
        if (defaults != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaults, java.nio.charset.StandardCharsets.UTF_8)));
        }
        return config;
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }

    private boolean setupStorage() {
        try {
            this.storage = databaseConfig.getStorageType() == DatabaseConfig.StorageType.MYSQL
                    ? new MySqlStorage(databaseConfig, getLogger())
                    : new SqliteStorage(databaseConfig, getLogger(), getDataFolder());
            storage.initialize();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Не удалось подключиться к " + databaseConfig.getStorageType(), e);
            return false;
        }

        try {
            if (databaseConfig.getCacheType() == DatabaseConfig.CacheType.REDIS) {
                this.cache = new RedisCache(databaseConfig, getLogger());
                cache.initialize();
            } else {
                this.cache = new MemoryCache(databaseConfig.getRedisEntryTtlSeconds() * 1000L);
                cache.initialize();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Redis недоступен, используется локальный кэш", e);
            this.cache = new MemoryCache(databaseConfig.getRedisEntryTtlSeconds() * 1000L);
            try {
                cache.initialize();
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    private void registerCommands() {
        AdminCommands admin = new AdminCommands(this);
        PlayerCommands player = new PlayerCommands(this);
        getCommand("strictkits").setExecutor(admin);
        getCommand("strictkits").setTabCompleter(admin);
        getCommand("kit").setExecutor(player);
        getCommand("kit").setTabCompleter(player);
    }

    private void runLegacyImport() {
        if (!databaseConfig.isImportLegacyYaml()) {
            return;
        }
        File folder = new File(getDataFolder(), "Cooldowns");
        LegacyImporter importer = new LegacyImporter(folder, storage, getLogger());
        if (!importer.hasLegacyData()) {
            return;
        }
        players.getWorkers().execute(importer::run);
    }

    private void preloadOnlinePlayers() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            players.getWorkers().execute(() -> players.loadBlocking(online.getUniqueId()));
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        FileConfiguration config = getConfig();
        this.settings = new Settings(config);
        this.lang = new Lang(config);
        GUItems.load(config);
        Messenger.detect();
        kits.flushAllBlocking();
        kits.loadAll();
        this.kitService = new KitService(kits, players, lang, settings);
        this.kitMenu = new KitMenu(kits, kitService, players, lang, settings);
        registerCommands();
    }

    public static Main getInstance() {
        return instance;
    }

    public Settings getSettings() {
        return settings;
    }

    public Lang getLang() {
        return lang;
    }

    public KitManager getKits() {
        return kits;
    }

    public KitService getKitService() {
        return kitService;
    }

    public KitMenu getKitMenu() {
        return kitMenu;
    }

    public PlayerDataManager getPlayers() {
        return players;
    }

    public String getStorageName() {
        return storage == null ? "none" : storage.name();
    }

    public String getCacheName() {
        return cache == null ? "none" : cache.name();
    }
}
