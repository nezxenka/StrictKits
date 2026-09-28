package org.nezxenka.strictkits.config.settings;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public final class Settings {

    private final MenuSettings menu;
    private final AccessSettings access;
    private final CommandSettings command;

    public Settings(FileConfiguration config) {
        this.menu = new MenuSettings(config);
        this.access = new AccessSettings(config);
        this.command = new CommandSettings(config);
    }
}
