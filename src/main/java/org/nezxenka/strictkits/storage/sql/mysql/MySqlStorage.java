package org.nezxenka.strictkits.storage.sql.mysql;

import com.zaxxer.hikari.HikariConfig;
import org.nezxenka.strictkits.config.database.DatabaseSettings;
import org.nezxenka.strictkits.config.database.MySqlSettings;
import org.nezxenka.strictkits.config.database.PoolSettings;
import org.nezxenka.strictkits.storage.sql.SqlStorage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public final class MySqlStorage extends SqlStorage {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    public MySqlStorage(DatabaseSettings config, Logger logger) {
        super(config, logger);
    }

    @Override
    public String name() {
        return "MySQL";
    }

    @Override
    protected void configurePool(HikariConfig hikari) {
        MySqlSettings mysql = config.getMysql();
        PoolSettings pool = mysql.getPool();
        StringBuilder url = new StringBuilder(128)
                .append("jdbc:mysql://").append(mysql.getHost()).append(':').append(mysql.getPort())
                .append('/').append(mysql.getDatabase())
                .append("?useSSL=").append(mysql.isUseSsl());
        mysql.getProperties().forEach((key, value) -> url.append('&').append(key).append('=').append(value));

        hikari.setDriverClassName(DRIVER);
        hikari.setJdbcUrl(url.toString());
        hikari.setUsername(mysql.getUsername());
        hikari.setPassword(mysql.getPassword());
        hikari.setMaximumPoolSize(pool.getMaximumSize());
        hikari.setMinimumIdle(pool.getMinimumIdle());
        hikari.setConnectionTimeout(pool.getConnectionTimeout());
        hikari.setMaxLifetime(pool.getMaxLifetime());
    }

    @Override
    protected String upsertCooldownStatement() {
        return MySqlSchema.upsert(queries.getCooldownTable(), "used_at");
    }

    @Override
    protected String upsertClaimStatement() {
        return MySqlSchema.upsert(queries.getClaimTable(), "claimed_at");
    }

    @Override
    protected List<String> schemaStatements() {
        return List.of(
                MySqlSchema.createTable(queries.getCooldownTable(), "used_at"),
                MySqlSchema.createTable(queries.getClaimTable(), "claimed_at"));
    }

    @Override
    protected void migrate(Connection connection) throws SQLException {
        new MySqlCollationMigration(logger).apply(connection, List.of(queries.getCooldownTable(), queries.getClaimTable()));
    }
}
