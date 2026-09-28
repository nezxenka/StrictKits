package org.nezxenka.strictkits.config.database;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public final class PoolSettings {

    private static final String PATH = "storage.mysql.pool.";

    private final int maximumSize;
    private final int minimumIdle;
    private final long connectionTimeout;
    private final long maxLifetime;

    public PoolSettings(FileConfiguration config) {
        this.maximumSize = Math.max(1, config.getInt(PATH + "maximum-pool-size", 4));
        this.minimumIdle = Math.min(maximumSize, Math.max(0, config.getInt(PATH + "minimum-idle", 1)));
        this.connectionTimeout = config.getLong(PATH + "connection-timeout", 5000L);
        this.maxLifetime = config.getLong(PATH + "max-lifetime", 1800000L);
    }
}
