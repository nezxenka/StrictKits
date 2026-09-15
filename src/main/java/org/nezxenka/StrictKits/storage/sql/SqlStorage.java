package org.nezxenka.StrictKits.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.nezxenka.StrictKits.storage.DataEntry;
import org.nezxenka.StrictKits.storage.DatabaseConfig;
import org.nezxenka.StrictKits.storage.PlayerRecord;
import org.nezxenka.StrictKits.storage.StorageProvider;

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

    protected final DatabaseConfig config;
    protected final Logger logger;
    protected final String cooldownTable;
    protected final String claimTable;

    private final String selectCooldowns;
    private final String selectClaims;
    private final String deleteKitCooldowns;
    private final String deleteKitClaims;
    private final String purgeCooldowns;

    private HikariDataSource dataSource;

    protected SqlStorage(DatabaseConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.cooldownTable = config.getTablePrefix() + "cooldowns";
        this.claimTable = config.getTablePrefix() + "claims";
        this.selectCooldowns = "SELECT kit, used_at FROM " + cooldownTable + " WHERE uuid = ?";
        this.selectClaims = "SELECT kit FROM " + claimTable + " WHERE uuid = ?";
        this.deleteKitCooldowns = "DELETE FROM " + cooldownTable + " WHERE kit = ?";
        this.deleteKitClaims = "DELETE FROM " + claimTable + " WHERE kit = ?";
        this.purgeCooldowns = "DELETE FROM " + cooldownTable + " WHERE kit = ? AND used_at < ?";
    }

    protected abstract void configurePool(HikariConfig hikari);

    protected abstract String upsertCooldownStatement();

    protected abstract String upsertClaimStatement();

    protected abstract List<String> schemaStatements();

    @Override
    public void initialize() throws SQLException {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("StrictKits-" + name());
        hikari.setConnectionTimeout(config.getPoolConnectionTimeout());
        hikari.setIdleTimeout(config.getPoolIdleTimeout());
        hikari.setMaxLifetime(config.getPoolMaxLifetime());
        hikari.setKeepaliveTime(config.getPoolKeepaliveTime());
        hikari.setLeakDetectionThreshold(config.getPoolLeakDetectionThreshold());
        hikari.setInitializationFailTimeout(-1L);
        configurePool(hikari);
        dataSource = new HikariDataSource(hikari);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schemaStatements()) {
                statement.execute(sql);
            }
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
            try (PreparedStatement statement = connection.prepareStatement(selectCooldowns)) {
                statement.setString(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        cooldowns.put(result.getString(1), result.getLong(2));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(selectClaims)) {
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

    private void write(String sql, List<DataEntry> entries) throws SQLException {
        if (entries.isEmpty()) {
            return;
        }
        int batchSize = config.getBatchSize();
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int pending = 0;
                for (DataEntry entry : entries) {
                    statement.setString(1, entry.getUuid().toString());
                    statement.setString(2, entry.getKit());
                    statement.setLong(3, entry.getTimestamp());
                    statement.addBatch();
                    if (++pending >= batchSize) {
                        statement.executeBatch();
                        pending = 0;
                    }
                }
                if (pending > 0) {
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    @Override
    public void deleteKit(String kit) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            deleteByKit(connection, deleteKitCooldowns, kit);
            deleteByKit(connection, deleteKitClaims, kit);
        }
    }

    private static void deleteByKit(Connection connection, String sql, String kit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, kit);
            statement.executeUpdate();
        }
    }

    @Override
    public int purgeCooldowns(String kit, long cutoff) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(purgeCooldowns)) {
            statement.setString(1, kit);
            statement.setLong(2, cutoff);
            return statement.executeUpdate();
        }
    }
}
