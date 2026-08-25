package org.nezxenka.StrictKits.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import org.nezxenka.StrictKits.storage.DatabaseConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            hikari.setDriverClassName("com.mysql.jdbc.Driver");
        }
        StringBuilder url = new StringBuilder(128);
        url.append("jdbc:mysql://").append(config.getMysqlHost()).append(':').append(config.getMysqlPort())
                .append('/').append(config.getMysqlDatabase())
                .append("?useSSL=").append(config.isMysqlUseSsl());
        for (Map.Entry<String, String> entry : config.getMysqlProperties().entrySet()) {
            url.append('&').append(entry.getKey()).append('=').append(entry.getValue());
        }
        hikari.setJdbcUrl(url.toString());
        hikari.setUsername(config.getMysqlUsername());
        hikari.setPassword(config.getMysqlPassword());
        hikari.setMaximumPoolSize(config.getPoolMaximumSize());
        hikari.setMinimumIdle(config.getPoolMinimumIdle());
    }

    @Override
    protected String upsertCooldownStatement() {
        return "INSERT INTO " + cooldownTable + " (uuid, kit, used_at) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE used_at = VALUES(used_at)";
    }

    @Override
    protected String upsertClaimStatement() {
        return "INSERT INTO " + claimTable + " (uuid, kit, claimed_at) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE claimed_at = VALUES(claimed_at)";
    }

    @Override
    protected List<String> schemaStatements() {
        List<String> statements = new ArrayList<>(2);
        statements.add("CREATE TABLE IF NOT EXISTS " + cooldownTable + " ("
                + "uuid CHAR(36) NOT NULL, "
                + "kit VARCHAR(64) NOT NULL, "
                + "used_at BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, kit), "
                + "INDEX idx_kit (kit)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        statements.add("CREATE TABLE IF NOT EXISTS " + claimTable + " ("
                + "uuid CHAR(36) NOT NULL, "
                + "kit VARCHAR(64) NOT NULL, "
                + "claimed_at BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, kit), "
                + "INDEX idx_kit (kit)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        return statements;
    }
}
