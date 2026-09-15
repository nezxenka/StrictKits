package org.nezxenka.StrictKits;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.nezxenka.StrictKits.commands.AdminCommands;
import org.nezxenka.StrictKits.commands.PlayerCommands;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.config.Settings;
import org.nezxenka.StrictKits.gui.KitMenu;
import org.nezxenka.StrictKits.gui.MenuHolder;
import org.nezxenka.StrictKits.gui.MenuItems;
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
import org.nezxenka.StrictKits.util.Messenger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public final class Main extends JavaPlugin {

    private static final String DATABASE_FILE = "database.yml";
    private static final String MESSAGES_FILE = "messages.yml";

    private Settings settings;
    private Messages messages;
    @Getter(AccessLevel.NONE)
    private MenuItems menuItems;
    @Getter(AccessLevel.NONE)
    private DatabaseConfig databaseConfig;
    private StorageProvider storage;
    private CacheProvider cache;
    private PlayerDataManager players;
    private KitManager kits;
    private KitService kitService;
    private KitMenu kitMenu;
    @Getter(AccessLevel.NONE)
    private BukkitTask menuRefreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing(DATABASE_FILE);

        loadConfiguration();
        databaseConfig = new DatabaseConfig(loadYaml(DATABASE_FILE, false));

        if (!setupStorage()) {
            getLogger().severe("Хранилище не инициализировано, плагин отключается");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupCache();

        players = new PlayerDataManager(storage, cache, databaseConfig, getLogger());
        players.start();

        kits = new KitManager(new KitStorage(new File(getDataFolder(), "Kits"), getLogger()));
        kits.loadAll();
        warnAboutMissingIcons();

        buildServices();
        startMenuRefresh();

        bindCommand("strictkits", new AdminCommands(this));
        bindCommand("kit", new PlayerCommands(this));
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        runStartupTasks();

        getLogger().info("StrictKits " + getDescription().getVersion() + " включен");
        getLogger().info("Хранилище: " + storage.name() + ", кэш: " + cache.name() + ", китов: " + kits.size());
    }

    @Override
    public void onDisable() {
        cancelMenuRefresh();
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

    public void reloadPlugin() {
        closeOpenMenus();
        reloadConfig();
        loadConfiguration();
        kits.flushAllBlocking();
        kits.loadAll();
        warnAboutMissingIcons();
        buildServices();
        startMenuRefresh();
    }

    private void loadConfiguration() {
        saveResourceIfMissing(MESSAGES_FILE);
        FileConfiguration config = getConfig();
        settings = new Settings(config);
        messages = new Messages(loadYaml(MESSAGES_FILE, true), getDescription().getVersion());
        menuItems = new MenuItems(config, messages);
        Messenger.detect();
    }

    private void buildServices() {
        kitService = new KitService(kits, players, messages);
        kitMenu = new KitMenu(kits, kitService, players, messages, settings, menuItems);
    }

    private YamlConfiguration loadYaml(String name, boolean completeMissingKeys) {
        File file = new File(getDataFolder(), name);
        YamlConfiguration config = new YamlConfiguration();
        boolean readable = readInto(config, file);
        Configuration defaults = jarDefaults(name);
        config.setDefaults(defaults);
        if (completeMissingKeys && readable && hasMissingKeys(config, defaults)) {
            config.options().copyDefaults(true);
            try {
                config.save(file);
                getLogger().info(name + " дополнен новыми ключами, комментарии в нём не сохраняются");
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Не удалось обновить " + name, e);
            }
        }
        return config;
    }

    private boolean readInto(YamlConfiguration config, File file) {
        if (!file.exists()) {
            return true;
        }
        try {
            config.load(file);
            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Не удалось прочитать " + file.getName() + ", применяются значения по умолчанию", e);
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Синтаксическая ошибка в " + file.getName() + ", применяются значения по умолчанию");
            getLogger().severe(e.getMessage());
        }
        return false;
    }

    private Configuration jarDefaults(String name) {
        try (Reader reader = Objects.requireNonNull(getTextResource(name), name)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean hasMissingKeys(YamlConfiguration config, Configuration defaults) {
        return defaults.getKeys(true).stream().anyMatch(key -> !config.contains(key, true));
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }

    private void warnAboutMissingIcons() {
        String missing = kits.all().stream()
                .filter(kit -> kit.getIcon() == null)
                .map(Kit::getName)
                .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            getLogger().warning("Без иконки (используется стандартная, задайте через /sk seticon): " + missing);
        }
    }

    private boolean setupStorage() {
        storage = databaseConfig.getStorageType() == DatabaseConfig.StorageType.MYSQL
                ? new MySqlStorage(databaseConfig, getLogger())
                : new SqliteStorage(databaseConfig, getLogger(), getDataFolder());
        try {
            storage.initialize();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Не удалось подключиться к " + databaseConfig.getStorageType(), e);
            return false;
        }
    }

    private void setupCache() {
        if (databaseConfig.getCacheType() == DatabaseConfig.CacheType.REDIS) {
            RedisCache redis = new RedisCache(databaseConfig, getLogger());
            try {
                redis.initialize();
                cache = redis;
                return;
            } catch (Exception e) {
                redis.shutdown();
                getLogger().log(Level.SEVERE, "Redis недоступен, используется локальный кэш", e);
            }
        }
        cache = new MemoryCache(databaseConfig.getMemoryEntryTtlMillis());
    }

    private void bindCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Команда " + name + " не объявлена в plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void startMenuRefresh() {
        cancelMenuRefresh();
        int ticks = settings.getGuiRefreshTicks();
        if (ticks > 0) {
            menuRefreshTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshOpenMenus, ticks, ticks);
        }
    }

    private void cancelMenuRefresh() {
        if (menuRefreshTask != null) {
            menuRefreshTask.cancel();
            menuRefreshTask = null;
        }
    }

    private void refreshOpenMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Inventory top = online.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof MenuHolder holder) {
                kitMenu.refresh(online, holder, top);
            }
        }
    }

    private void closeOpenMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                online.closeInventory();
            }
        }
    }

    private void runStartupTasks() {
        List<UUID> online = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList();
        File legacyFolder = new File(getDataFolder(), "Cooldowns");
        boolean importLegacy = databaseConfig.isImportLegacyYaml() && LegacyImporter.hasLegacyData(legacyFolder);
        players.getWorkers().execute(() -> {
            if (importLegacy) {
                new LegacyImporter(legacyFolder, storage, getLogger(), knownPlayerNames()).run();
            }
            online.forEach(players::markOnline);
        });
    }

    private static Map<String, UUID> knownPlayerNames() {
        Map<String, UUID> known = new HashMap<>();
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String name = offline.getName();
            if (name != null) {
                known.put(name.toLowerCase(), offline.getUniqueId());
            }
        }
        return known;
    }
}
