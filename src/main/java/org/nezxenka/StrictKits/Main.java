package org.nezxenka.StrictKits;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.nezxenka.StrictKits.commands.AdminCommands;
import org.nezxenka.StrictKits.commands.PlayerCommands;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.config.Settings;
import org.nezxenka.StrictKits.gui.KitMenu;
import org.nezxenka.StrictKits.gui.MenuHolder;
import org.nezxenka.StrictKits.kit.Kit;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private DatabaseConfig databaseConfig;
    private StorageProvider storage;
    private CacheProvider cache;
    private PlayerDataManager players;
    private KitManager kits;
    private KitService kitService;
    private KitMenu kitMenu;
    private BukkitTask menuRefreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("database.yml");

        loadConfiguration();
        this.databaseConfig = new DatabaseConfig(readYaml("database.yml"));
        Messenger.detect();

        if (!setupStorage()) {
            getLogger().severe("Хранилище не инициализировано, плагин отключается");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.players = new PlayerDataManager(storage, cache, databaseConfig, getLogger());
        this.players.start();

        this.kits = new KitManager(new KitStorage(new File(getDataFolder(), "Kits"), getLogger()));
        int loaded = kits.loadAll();
        warnAboutMissingIcons();

        buildServices();
        startMenuRefresh();

        registerCommands();
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        runStartupTasks();

        getLogger().info("StrictKits " + getDescription().getVersion() + " включен");
        getLogger().info("Хранилище: " + storage.name() + ", кэш: " + cache.name() + ", китов: " + loaded);
    }

    @Override
    public void onDisable() {
        if (menuRefreshTask != null) {
            menuRefreshTask.cancel();
            menuRefreshTask = null;
        }
        if (kits != null) {
            kits.flushAllBlocking();
            kits.shutdown();
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
        getLogger().info("StrictKits выключен");
    }

    private void loadConfiguration() {
        FileConfiguration config = getConfig();
        this.settings = new Settings(config);
        this.messages = new Messages(loadMessages(), getDescription().getVersion());
        GUItems.load(config, messages);
    }

    private void buildServices() {
        this.kitService = new KitService(kits, players, messages, settings);
        this.kitMenu = new KitMenu(kits, kitService, players, messages, settings);
    }

    private FileConfiguration loadMessages() {
        saveResourceIfMissing("messages.yml");
        File file = new File(getDataFolder(), "messages.yml");
        YamlConfiguration config = new YamlConfiguration();
        boolean readable = readInto(config, file, "messages.yml");
        Configuration defaults = jarDefaults("messages.yml");
        if (defaults != null) {
            config.setDefaults(defaults);
        }
        if (!readable || defaults == null || !hasMissingKeys(config, defaults)) {
            return config;
        }
        config.options().copyDefaults(true);
        try {
            config.save(file);
            getLogger().info("messages.yml дополнен новыми ключами, комментарии в нём не сохраняются");
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Не удалось обновить messages.yml", e);
        }
        return config;
    }

    private YamlConfiguration readYaml(String name) {
        YamlConfiguration config = new YamlConfiguration();
        readInto(config, new File(getDataFolder(), name), name);
        Configuration defaults = jarDefaults(name);
        if (defaults != null) {
            config.setDefaults(defaults);
        }
        return config;
    }

    private boolean readInto(YamlConfiguration config, File file, String name) {
        if (!file.exists()) {
            return true;
        }
        try {
            config.load(file);
            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Не удалось прочитать " + name + ", применяются значения по умолчанию", e);
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Синтаксическая ошибка в " + name + ", применяются значения по умолчанию");
            getLogger().severe(e.getMessage());
        }
        return false;
    }

    private Configuration jarDefaults(String name) {
        InputStream resource = getResource(name);
        if (resource == null) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
    }

    private static boolean hasMissingKeys(YamlConfiguration config, Configuration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!config.contains(key, true)) {
                return true;
            }
        }
        return false;
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }

    private void warnAboutMissingIcons() {
        List<String> missing = new ArrayList<>();
        for (Kit kit : kits.all()) {
            if (kit.getIcon() == null) {
                missing.add(kit.getName());
            }
        }
        if (!missing.isEmpty()) {
            getLogger().warning("Без иконки (используется стандартная, задайте через /sk seticon): "
                    + String.join(", ", missing));
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

        if (databaseConfig.getCacheType() == DatabaseConfig.CacheType.REDIS) {
            RedisCache redis = new RedisCache(databaseConfig, getLogger());
            try {
                redis.initialize();
                this.cache = redis;
                return true;
            } catch (Exception e) {
                redis.shutdown();
                getLogger().log(Level.SEVERE, "Redis недоступен, используется локальный кэш", e);
            }
        }

        MemoryCache memory = new MemoryCache(databaseConfig.getMemoryEntryTtlMillis());
        memory.initialize();
        this.cache = memory;
        return true;
    }

    private void registerCommands() {
        AdminCommands admin = new AdminCommands(this);
        PlayerCommands player = new PlayerCommands(this);
        if (getCommand("strictkits") != null) {
            getCommand("strictkits").setExecutor(admin);
            getCommand("strictkits").setTabCompleter(admin);
        }
        if (getCommand("kit") != null) {
            getCommand("kit").setExecutor(player);
            getCommand("kit").setTabCompleter(player);
        }
    }

    private void startMenuRefresh() {
        if (menuRefreshTask != null) {
            menuRefreshTask.cancel();
            menuRefreshTask = null;
        }
        int ticks = settings.getGuiRefreshTicks();
        if (ticks <= 0) {
            return;
        }
        menuRefreshTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshOpenMenus, ticks, ticks);
    }

    private void refreshOpenMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Inventory top = online.getOpenInventory().getTopInventory();
            InventoryHolder holder = top.getHolder();
            if (holder instanceof MenuHolder) {
                kitMenu.refresh(online, (MenuHolder) holder, top);
            }
        }
    }

    private void runStartupTasks() {
        List<UUID> online = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
        }
        File folder = new File(getDataFolder(), "Cooldowns");
        boolean hasLegacyFiles = databaseConfig.isImportLegacyYaml()
                && (new File(folder, "Cooldowns.yml").exists() || new File(folder, "OneTimeUseList.yml").exists());
        players.getWorkers().execute(() -> {
            if (hasLegacyFiles) {
                Map<String, UUID> known = new HashMap<>();
                for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                    String name = offline.getName();
                    if (name != null) {
                        known.put(name.toLowerCase(), offline.getUniqueId());
                    }
                }
                new LegacyImporter(folder, storage, getLogger(), known).run();
            }
            for (UUID uuid : online) {
                players.markOnline(uuid);
            }
        });
    }

    public void reloadPlugin() {
        closeOpenMenus();
        reloadConfig();
        loadConfiguration();
        Messenger.detect();
        kits.flushAllBlocking();
        kits.loadAll();
        warnAboutMissingIcons();
        buildServices();
        startMenuRefresh();
    }

    private void closeOpenMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                online.closeInventory();
            }
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public Messages getMessages() {
        return messages;
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
