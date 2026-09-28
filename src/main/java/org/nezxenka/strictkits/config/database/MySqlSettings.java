package org.nezxenka.strictkits.config.database;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class MySqlSettings {

    private static final String PATH = "storage.mysql.";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final Map<String, String> properties;
    private final PoolSettings pool;

    public MySqlSettings(FileConfiguration config) {
        this.host = config.getString(PATH + "host", "127.0.0.1");
        this.port = config.getInt(PATH + "port", 3306);
        this.database = config.getString(PATH + "database", "strictkits");
        this.username = config.getString(PATH + "username", "root");
        this.password = config.getString(PATH + "password", "");
        this.useSsl = config.getBoolean(PATH + "use-ssl", false);
        this.properties = readProperties(config.getConfigurationSection(PATH + "properties"));
        this.pool = new PoolSettings(config);
    }

    private static Map<String, String> readProperties(ConfigurationSection section) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                properties.put(key, String.valueOf(section.get(key)));
            }
        }
        return Collections.unmodifiableMap(properties);
    }
}
