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
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
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
        hikari.setConnectionInitSql("PRAGMA journal_mode=" + config.getSqliteJournalMode()
                + "; PRAGMA synchronous=" + config.getSqliteSynchronous() + ";");
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
        List<String> statements = new ArrayList<>(4);
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
