package org.nezxenka.StrictKits.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import org.nezxenka.StrictKits.storage.DatabaseConfig;

import java.util.List;
import java.util.logging.Logger;

public final class MySqlStorage extends SqlStorage {

    public MySqlStorage(DatabaseConfig config, Logger logger) {
        super(config, logger);
    }

    @Override
    public String name() {
        return "MySQL";
    }

    @Override
    protected void configurePool(HikariConfig hikari) {
        StringBuilder url = new StringBuilder(128)
                .append("jdbc:mysql://").append(config.getMysqlHost()).append(':').append(config.getMysqlPort())
                .append('/').append(config.getMysqlDatabase())
                .append("?useSSL=").append(config.isMysqlUseSsl());
        config.getMysqlProperties().forEach((key, value) -> url.append('&').append(key).append('=').append(value));

        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(url.toString());
        hikari.setUsername(config.getMysqlUsername());
        hikari.setPassword(config.getMysqlPassword());
        hikari.setMaximumPoolSize(config.getPoolMaximumSize());
        hikari.setMinimumIdle(config.getPoolMinimumIdle());
    }

    @Override
    protected String upsertCooldownStatement() {
        return upsert(cooldownTable, "used_at");
    }

    @Override
    protected String upsertClaimStatement() {
        return upsert(claimTable, "claimed_at");
    }

    @Override
    protected List<String> schemaStatements() {
        return List.of(table(cooldownTable, "used_at"), table(claimTable, "claimed_at"));
    }

    private static String upsert(String table, String timeColumn) {
        return "INSERT INTO " + table + " (uuid, kit, " + timeColumn + ") VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE " + timeColumn + " = VALUES(" + timeColumn + ")";
    }

    private static String table(String table, String timeColumn) {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid CHAR(36) NOT NULL, "
                + "kit VARCHAR(64) NOT NULL, "
                + timeColumn + " BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, kit), "
                + "INDEX idx_kit (kit)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    }
}
