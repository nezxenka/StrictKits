package org.nezxenka.StrictKits.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import org.nezxenka.StrictKits.storage.DatabaseConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class SqliteStorage extends SqlStorage {

    private final File file;

    public SqliteStorage(DatabaseConfig config, Logger logger, File dataFolder) {
        super(config, logger);
        this.file = new File(dataFolder, config.getSqliteFile());
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
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setMinimumIdle(1);
        hikari.setMaxLifetime(0L);
        hikari.setKeepaliveTime(0L);
        hikari.setConnectionInitSql("PRAGMA synchronous=" + pragma(config.getSqliteSynchronous(), "NORMAL"));
    }

    private static String pragma(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_') {
                builder.append(c);
            }
        }
        return builder.length() == 0 ? fallback : builder.toString();
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
        List<String> statements = new ArrayList<>(5);
        statements.add("PRAGMA journal_mode=" + pragma(config.getSqliteJournalMode(), "WAL"));
        statements.add("CREATE TABLE IF NOT EXISTS " + cooldownTable + " ("
                + "uuid TEXT NOT NULL, "
                + "kit TEXT NOT NULL, "
                + "used_at INTEGER NOT NULL, "
                + "PRIMARY KEY (uuid, kit))");
        statements.add("CREATE TABLE IF NOT EXISTS " + claimTable + " ("
                + "uuid TEXT NOT NULL, "
                + "kit TEXT NOT NULL, "
                + "claimed_at INTEGER NOT NULL, "
                + "PRIMARY KEY (uuid, kit))");
        statements.add("CREATE INDEX IF NOT EXISTS idx_" + cooldownTable + "_kit ON " + cooldownTable + " (kit)");
        statements.add("CREATE INDEX IF NOT EXISTS idx_" + claimTable + "_kit ON " + claimTable + " (kit)");
        return statements;
    }
}
