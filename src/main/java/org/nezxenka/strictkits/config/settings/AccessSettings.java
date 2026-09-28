package org.nezxenka.strictkits.config.settings;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public final class AccessSettings {

    private final boolean listRequiresPermission;
    private final boolean previewRequiresPermission;

    public AccessSettings(FileConfiguration config) {
        this.listRequiresPermission = config.getBoolean("permission.kit-list");
        this.previewRequiresPermission = config.getBoolean("permission.kit-preview");
    }
}
