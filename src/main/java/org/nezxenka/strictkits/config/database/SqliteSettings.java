package org.nezxenka.strictkits.config.database;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public final class SqliteSettings {

    private static final String PATH = "storage.sqlite.";

    private final String file;
    private final String journalMode;
    private final String synchronous;

    public SqliteSettings(FileConfiguration config) {
        this.file = config.getString(PATH + "file", "data.db");
        this.journalMode = config.getString(PATH + "journal-mode", "WAL");
        this.synchronous = config.getString(PATH + "synchronous", "NORMAL");
    }
}
