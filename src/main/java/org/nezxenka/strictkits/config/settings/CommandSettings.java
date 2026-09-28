package org.nezxenka.strictkits.config.settings;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public final class CommandSettings {

    private final long throttleMillis;

    public CommandSettings(FileConfiguration config) {
        this.throttleMillis = Math.max(0L, config.getLong("settings.command-throttle-millis"));
    }
}
