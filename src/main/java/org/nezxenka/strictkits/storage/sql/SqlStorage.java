package org.nezxenka.strictkits.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.nezxenka.strictkits.config.database.DatabaseSettings;
import org.nezxenka.strictkits.storage.api.StorageProvider;
import org.nezxenka.strictkits.storage.model.DataEntry;
import org.nezxenka.strictkits.storage.model.PlayerRecord;
import org.nezxenka.strictkits.storage.sql.query.SqlQueries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class SqlStorage implements StorageProvider {

    protected final DatabaseSettings config;
    protected final Logger logger;
    protected final SqlQueries queries;

    private HikariDataSource dataSource;

    protected SqlStorage(DatabaseSettings config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.queries = new SqlQueries(config.getTablePrefix());
    }

    protected abstract void configurePool(HikariConfig hikari);

    protected abstract String upsertCooldownStatement();

    protected abstract String upsertClaimStatement();

    protected abstract List<String> schemaStatements();

    protected void migrate(Connection connection) throws SQLException {
    }

    @Override
    public void initialize() throws SQLException {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("StrictKits-" + name());
        hikari.setInitializationFailTimeout(-1L);
        configurePool(hikari);
        dataSource = new HikariDataSource(hikari);

        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String sql : schemaStatements()) {
                    statement.execute(sql);
                }
            }
            migrate(connection);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public PlayerRecord load(UUID uuid) throws SQLException {
        Map<String, Long> cooldowns = new HashMap<>(8);
        Set<String> claims = new HashSet<>(8);
        String id = uuid.toString();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(queries.getSelectCooldowns())) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        cooldowns.put(result.getString(1), result.getLong(2));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(queries.getSelectClaims())) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        claims.add(result.getString(1));
                    }
                }
            }
        }
        return new PlayerRecord(uuid, cooldowns, claims);
    }

    @Override
    public void writeCooldowns(List<DataEntry> entries) throws SQLException {
        write(upsertCooldownStatement(), entries);
    }

    @Override
    public void writeClaims(List<DataEntry> entries) throws SQLException {
        write(upsertClaimStatement(), entries);
    }

    @Override
    public void deleteKit(String kit) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            deleteByKit(connection, queries.getDeleteKitCooldowns(), kit);
            deleteByKit(connection, queries.getDeleteKitClaims(), kit);
        }
    }

    @Override
    public int purgeCooldowns(String kit, long cutoff) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(queries.getPurgeCooldowns())) {
            statement.setString(1, kit);
            statement.setLong(2, cutoff);
            return statement.executeUpdate();
        }
    }

    private void write(String sql, List<DataEntry> entries) throws SQLException {
        if (entries.isEmpty()) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (DataEntry entry : entries) {
                    statement.setString(1, entry.getUuid().toString());
                    statement.setString(2, entry.getKit());
                    statement.setLong(3, entry.getTimestamp());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private static void deleteByKit(Connection connection, String sql, String kit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, kit);
            statement.executeUpdate();
        }
    }
}
