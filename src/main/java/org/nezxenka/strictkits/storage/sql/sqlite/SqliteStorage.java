package org.nezxenka.strictkits.storage.sql.sqlite;

import com.zaxxer.hikari.HikariConfig;
import org.nezxenka.strictkits.config.database.DatabaseSettings;
import org.nezxenka.strictkits.config.database.SqliteSettings;
import org.nezxenka.strictkits.storage.sql.SqlStorage;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public final class SqliteStorage extends SqlStorage {

    private static final String DRIVER = "org.sqlite.JDBC";
    private static final long CONNECTION_TIMEOUT_MILLIS = 5000L;

    private final File file;

    public SqliteStorage(DatabaseSettings config, Logger logger, File dataFolder) {
        super(config, logger);
        this.file = SqliteFileResolver.resolve(dataFolder, config.getSqlite().getFile(), logger);
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
        hikari.setDriverClassName(DRIVER);
        hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setMinimumIdle(1);
        hikari.setMaxLifetime(0L);
        hikari.setKeepaliveTime(0L);
        hikari.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
        hikari.setConnectionInitSql(SqliteSchema.synchronous(config.getSqlite().getSynchronous()));
    }

    @Override
    protected String upsertCooldownStatement() {
        return SqliteSchema.upsert(queries.getCooldownTable(), "used_at");
    }

    @Override
    protected String upsertClaimStatement() {
        return SqliteSchema.upsert(queries.getClaimTable(), "claimed_at");
    }

    @Override
    protected List<String> schemaStatements() {
        SqliteSettings sqlite = config.getSqlite();
        return List.of(
                SqliteSchema.journalMode(sqlite.getJournalMode()),
                SqliteSchema.createTable(queries.getCooldownTable(), "used_at"),
                SqliteSchema.createTable(queries.getClaimTable(), "claimed_at"),
                SqliteSchema.createKitIndex(queries.getCooldownTable()),
                SqliteSchema.createKitIndex(queries.getClaimTable()));
    }
}
