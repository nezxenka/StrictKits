package org.nezxenka.strictkits.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.nezxenka.strictkits.config.database.DatabaseSettings;
import org.nezxenka.strictkits.config.loader.YamlFiles;
import org.nezxenka.strictkits.config.menu.MenuItems;
import org.nezxenka.strictkits.config.settings.Settings;
import org.nezxenka.strictkits.message.Messages;
import org.nezxenka.strictkits.message.sender.Messenger;

@Getter
public final class ConfigManager {

    private static final String DATABASE_FILE = "database.yml";
    private static final String MESSAGES_FILE = "messages.yml";

    @Getter(AccessLevel.NONE)
    private final JavaPlugin plugin;
    @Getter(AccessLevel.NONE)
    private final YamlFiles files;

    private DatabaseSettings database;
    private Settings settings;
    private Messages messages;
    private MenuItems menuItems;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.files = new YamlFiles(plugin);
    }

    public void load() {
        plugin.saveDefaultConfig();
        files.saveIfMissing(DATABASE_FILE);
        database = new DatabaseSettings(files.load(DATABASE_FILE, false));
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        files.saveIfMissing(MESSAGES_FILE);
        FileConfiguration config = plugin.getConfig();
        settings = new Settings(config);
        messages = new Messages(files.load(MESSAGES_FILE, true), plugin.getDescription().getVersion());
        menuItems = new MenuItems(config, messages.getMenu());
        Messenger.detect();
    }
}
