package org.nezxenka.strictkits.config.settings;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public final class MenuSettings {

    private static final int MIN_REFRESH_TICKS = 10;
    private static final int MAX_ROWS = 6;

    private final boolean enabled;
    private final boolean previewOnRightClick;
    private final boolean showWithoutPermission;
    private final int refreshTicks;
    private final int rows;

    public MenuSettings(FileConfiguration config) {
        this.enabled = config.getBoolean("settings.enable-gui-kit-displaying");
        this.previewOnRightClick = config.getBoolean("settings.enable-gui-kit-previewing");
        this.showWithoutPermission = config.getBoolean("settings.kit-display-without-perm");
        int configuredTicks = config.getInt("settings.gui-refresh-ticks");
        this.refreshTicks = configuredTicks <= 0 ? 0 : Math.max(MIN_REFRESH_TICKS, configuredTicks);
        this.rows = Math.min(MAX_ROWS, Math.max(1, config.getInt("gui.rows")));
    }
}
