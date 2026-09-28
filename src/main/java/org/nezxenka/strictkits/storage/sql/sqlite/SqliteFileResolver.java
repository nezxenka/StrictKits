package org.nezxenka.strictkits.storage.sql.sqlite;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@UtilityClass
public class SqliteFileResolver {

    private static final String DEFAULT_FILE = "data.db";

    public static File resolve(File dataFolder, String path, Logger logger) {
        File requested = new File(dataFolder, path);
        try {
            String folder = dataFolder.getCanonicalPath();
            String target = requested.getCanonicalPath();
            if (target.startsWith(folder + File.separator)) {
                return requested;
            }
            logger.warning("Путь к SQLite вне папки плагина заблокирован: " + path + " -> используется " + DEFAULT_FILE);
        } catch (IOException e) {
            logger.warning("Не удалось проверить путь к БД: " + e.getMessage() + " -> используется " + DEFAULT_FILE);
        }
        return new File(dataFolder, DEFAULT_FILE);
    }
}
