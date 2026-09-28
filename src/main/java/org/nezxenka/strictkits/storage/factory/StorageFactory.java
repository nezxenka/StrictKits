package org.nezxenka.strictkits.storage.factory;

import lombok.experimental.UtilityClass;
import org.nezxenka.strictkits.config.database.DatabaseSettings;
import org.nezxenka.strictkits.storage.api.StorageProvider;
import org.nezxenka.strictkits.storage.sql.mysql.MySqlStorage;
import org.nezxenka.strictkits.storage.sql.sqlite.SqliteStorage;

import java.io.File;
import java.util.logging.Logger;

@UtilityClass
public class StorageFactory {

    public static StorageProvider create(DatabaseSettings settings, File dataFolder, Logger logger) {
        return switch (settings.getStorageType()) {
            case MYSQL -> new MySqlStorage(settings, logger);
            case SQLITE -> new SqliteStorage(settings, logger, dataFolder);
        };
    }
}
