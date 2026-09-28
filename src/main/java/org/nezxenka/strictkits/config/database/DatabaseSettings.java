package org.nezxenka.strictkits.config.database;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.nezxenka.strictkits.storage.api.StorageType;

import java.util.Locale;

@Getter
public final class DatabaseSettings {

    private static final String DEFAULT_TABLE_PREFIX = "strictkits_";

    private final StorageType storageType;
    private final String tablePrefix;
    private final SqliteSettings sqlite;
    private final MySqlSettings mysql;
    private final boolean importLegacyYaml;

    public DatabaseSettings(FileConfiguration config) {
        this.storageType = parseStorageType(config.getString("storage.type"));
        this.tablePrefix = sanitizeTablePrefix(config.getString("storage.table-prefix", DEFAULT_TABLE_PREFIX));
        this.sqlite = new SqliteSettings(config);
        this.mysql = new MySqlSettings(config);
        this.importLegacyYaml = config.getBoolean("migration.import-legacy-yaml", true);
    }

    private static StorageType parseStorageType(String raw) {
        if (raw == null) {
            return StorageType.SQLITE;
        }
        try {
            return StorageType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return StorageType.SQLITE;
        }
    }

    private static String sanitizeTablePrefix(String prefix) {
        String sanitized = prefix.replaceAll("[^\\p{L}\\p{Nd}_]", "");
        return sanitized.isEmpty() ? DEFAULT_TABLE_PREFIX : sanitized;
    }
}
