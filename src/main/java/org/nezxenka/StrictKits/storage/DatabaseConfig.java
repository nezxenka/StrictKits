package org.nezxenka.StrictKits.storage;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class DatabaseConfig {

    public enum StorageType {
        SQLITE,
        MYSQL
    }

    public enum CacheType {
        MEMORY,
        REDIS
    }

    private static final String DEFAULT_TABLE_PREFIX = "strictkits_";

    private final StorageType storageType;
    private final String tablePrefix;

    private final String sqliteFile;
    private final String sqliteJournalMode;
    private final String sqliteSynchronous;

    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final boolean mysqlUseSsl;
    private final Map<String, String> mysqlProperties;
    private final int poolMaximumSize;
    private final int poolMinimumIdle;
    private final long poolConnectionTimeout;
    private final long poolIdleTimeout;
    private final long poolMaxLifetime;
    private final long poolKeepaliveTime;
    private final long poolLeakDetectionThreshold;

    private final CacheType cacheType;
    private final long memoryEntryTtlMillis;
    private final String redisHost;
    private final int redisPort;
    private final String redisUsername;
    private final String redisPassword;
    private final int redisDatabase;
    private final boolean redisSsl;
    private final int redisTimeout;
    private final String redisKeyPrefix;
    private final int redisEntryTtlSeconds;
    private final String redisChannel;
    private final int redisMaxTotal;
    private final int redisMaxIdle;
    private final int redisMinIdle;
    private final long redisMaxWaitMillis;

    private final int flushIntervalSeconds;
    private final int batchSize;
    private final boolean saveOnQuit;
    private final int unloadDelaySeconds;
    private final long loadTimeoutMillis;
    private final int workerThreads;
    private final boolean importLegacyYaml;

    public DatabaseConfig(FileConfiguration config) {
        this.storageType = parseEnum(StorageType.class, config.getString("storage.type"), StorageType.SQLITE);
        this.tablePrefix = sanitizeTablePrefix(config.getString("storage.table-prefix", DEFAULT_TABLE_PREFIX));

        this.sqliteFile = config.getString("storage.sqlite.file", "data.db");
        this.sqliteJournalMode = config.getString("storage.sqlite.journal-mode", "WAL");
        this.sqliteSynchronous = config.getString("storage.sqlite.synchronous", "NORMAL");

        this.mysqlHost = config.getString("storage.mysql.host", "127.0.0.1");
        this.mysqlPort = config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = config.getString("storage.mysql.database", "strictkits");
        this.mysqlUsername = config.getString("storage.mysql.username", "root");
        this.mysqlPassword = config.getString("storage.mysql.password", "");
        this.mysqlUseSsl = config.getBoolean("storage.mysql.use-ssl", false);
        this.mysqlProperties = readProperties(config.getConfigurationSection("storage.mysql.properties"));
        this.poolMaximumSize = Math.max(1, config.getInt("storage.mysql.pool.maximum-pool-size", 10));
        this.poolMinimumIdle = Math.max(1, config.getInt("storage.mysql.pool.minimum-idle", 10));
        this.poolConnectionTimeout = config.getLong("storage.mysql.pool.connection-timeout", 5000L);
        this.poolIdleTimeout = config.getLong("storage.mysql.pool.idle-timeout", 600000L);
        this.poolMaxLifetime = config.getLong("storage.mysql.pool.max-lifetime", 1800000L);
        this.poolKeepaliveTime = config.getLong("storage.mysql.pool.keepalive-time", 0L);
        this.poolLeakDetectionThreshold = config.getLong("storage.mysql.pool.leak-detection-threshold", 0L);

        this.cacheType = parseEnum(CacheType.class, config.getString("cache.type"), CacheType.MEMORY);
        this.memoryEntryTtlMillis = Math.max(1, config.getInt("cache.memory.entry-ttl-seconds", 300)) * 1000L;
        this.redisHost = config.getString("cache.redis.host", "127.0.0.1");
        this.redisPort = config.getInt("cache.redis.port", 6379);
        this.redisUsername = config.getString("cache.redis.username", "");
        this.redisPassword = config.getString("cache.redis.password", "");
        this.redisDatabase = config.getInt("cache.redis.database", 0);
        this.redisSsl = config.getBoolean("cache.redis.ssl", false);
        this.redisTimeout = config.getInt("cache.redis.timeout", 2000);
        this.redisKeyPrefix = config.getString("cache.redis.key-prefix", "strictkits:");
        this.redisEntryTtlSeconds = Math.max(1, config.getInt("cache.redis.entry-ttl-seconds", 3600));
        this.redisChannel = config.getString("cache.redis.pubsub-channel", "strictkits:sync");
        this.redisMaxTotal = Math.max(1, config.getInt("cache.redis.pool.max-total", 16));
        this.redisMaxIdle = Math.max(1, config.getInt("cache.redis.pool.max-idle", 8));
        this.redisMinIdle = Math.max(0, config.getInt("cache.redis.pool.min-idle", 2));
        this.redisMaxWaitMillis = config.getLong("cache.redis.pool.max-wait-millis", 2000L);

        this.flushIntervalSeconds = Math.max(1, config.getInt("sync.flush-interval-seconds", 30));
        this.batchSize = Math.max(16, config.getInt("sync.batch-size", 500));
        this.saveOnQuit = config.getBoolean("sync.save-on-quit", true);
        this.unloadDelaySeconds = Math.max(0, config.getInt("sync.unload-delay-seconds", 60));
        this.loadTimeoutMillis = Math.max(500L, config.getLong("sync.load-timeout-millis", 4000L));
        this.workerThreads = Math.max(1, config.getInt("sync.worker-threads", 4));
        this.importLegacyYaml = config.getBoolean("migration.import-legacy-yaml", true);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String sanitizeTablePrefix(String prefix) {
        String sanitized = prefix.replaceAll("[^\\p{L}\\p{Nd}_]", "");
        return sanitized.isEmpty() ? DEFAULT_TABLE_PREFIX : sanitized;
    }

    private static Map<String, String> readProperties(ConfigurationSection section) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                properties.put(key, String.valueOf(section.get(key)));
            }
        }
        return Collections.unmodifiableMap(properties);
    }
}
