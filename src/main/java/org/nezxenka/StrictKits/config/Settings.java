package org.nezxenka.StrictKits.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class Settings {

    private final boolean guiDisplay;
    private final boolean guiPreview;
    private final boolean displayWithoutPermission;
    private final boolean listRequiresPermission;
    private final boolean previewRequiresPermission;
    private final long commandThrottleMillis;
    private final int guiRefreshTicks;
    private final int guiRows;

    public Settings(FileConfiguration config) {
        this.guiDisplay = config.getBoolean("settings.enable-gui-kit-displaying", false);
        this.guiPreview = config.getBoolean("settings.enable-gui-kit-previewing", false);
        this.displayWithoutPermission = config.getBoolean("settings.kit-display-without-perm", true);
        this.listRequiresPermission = config.getBoolean("permission.kit-list", false);
        this.previewRequiresPermission = config.getBoolean("permission.kit-preview", false);
        this.commandThrottleMillis = Math.max(0L, config.getLong("settings.command-throttle-millis", 250L));
        this.guiRefreshTicks = Math.max(0, config.getInt("settings.gui-refresh-ticks", 40));
        this.guiRows = Math.min(6, Math.max(1, config.getInt("gui.rows", 6)));
    }

    public boolean isGuiDisplay() {
        return guiDisplay;
    }

    public boolean isGuiPreview() {
        return guiPreview;
    }

    public boolean isDisplayWithoutPermission() {
        return displayWithoutPermission;
    }

    public boolean isListRequiresPermission() {
        return listRequiresPermission;
    }

    public boolean isPreviewRequiresPermission() {
        return previewRequiresPermission;
    }

    public long getCommandThrottleMillis() {
        return commandThrottleMillis;
    }

    public int getGuiRefreshTicks() {
        return guiRefreshTicks;
    }

    public int getGuiRows() {
        return guiRows;
    }
}
