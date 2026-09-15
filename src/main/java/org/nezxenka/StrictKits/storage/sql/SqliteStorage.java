package org.nezxenka.StrictKits.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import org.nezxenka.StrictKits.storage.DatabaseConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public final class SqliteStorage extends SqlStorage {

    private static final String DEFAULT_FILE = "data.db";

    private final File file;

    public SqliteStorage(DatabaseConfig config, Logger logger, File dataFolder) {
        super(config, logger);
        this.file = resolveFile(dataFolder, config.getSqliteFile(), logger);
    }

    private static File resolveFile(File dataFolder, String path, Logger logger) {
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

    @Override
    public String name() {
        return "SQLite";
    }

    @Override
    protected void configurePool(HikariConfig hikari) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warning("Не удалось создать папку для базы " + parent.getPath());
        }
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setMinimumIdle(1);
        hikari.setMaxLifetime(0L);
        hikari.setKeepaliveTime(0L);
        hikari.setConnectionInitSql("PRAGMA synchronous=" + pragma(config.getSqliteSynchronous(), "NORMAL"));
    }

    @Override
    protected String upsertCooldownStatement() {
        return "INSERT OR REPLACE INTO " + cooldownTable + " (uuid, kit, used_at) VALUES (?, ?, ?)";
    }

    @Override
    protected String upsertClaimStatement() {
        return "INSERT OR REPLACE INTO " + claimTable + " (uuid, kit, claimed_at) VALUES (?, ?, ?)";
    }

    @Override
    protected List<String> schemaStatements() {
        return List.of(
                "PRAGMA journal_mode=" + pragma(config.getSqliteJournalMode(), "WAL"),
                table(cooldownTable, "used_at"),
                table(claimTable, "claimed_at"),
                index(cooldownTable),
                index(claimTable));
    }

    private static String table(String table, String timeColumn) {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid TEXT NOT NULL, "
                + "kit TEXT NOT NULL, "
                + timeColumn + " INTEGER NOT NULL, "
                + "PRIMARY KEY (uuid, kit))";
    }

    private static String index(String table) {
        return "CREATE INDEX IF NOT EXISTS idx_" + table + "_kit ON " + table + " (kit)";
    }

    private static String pragma(String raw, String fallback) {
        String value = raw == null ? "" : raw.replaceAll("[^A-Za-z_]", "");
        return value.isEmpty() ? fallback : value;
    }
}
