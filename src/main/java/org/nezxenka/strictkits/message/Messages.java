package org.nezxenka.strictkits.message;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.nezxenka.strictkits.message.format.DurationFormat;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.section.MenuMessages;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.template.MessageReader;

@Getter
public final class Messages {

    private static final String PREFIX_PATH = "admin.prefix";

    private final PlayerMessages player;
    private final MenuMessages menu;
    private final AdminMessages admin;
    private final DurationFormat durationFormat;

    public Messages(FileConfiguration config, String pluginVersion) {
        MessageReader reader = new MessageReader(config, PREFIX_PATH);
        this.player = new PlayerMessages(reader);
        this.menu = new MenuMessages(reader);
        this.admin = new AdminMessages(reader, pluginVersion);
        this.durationFormat = new DurationFormat(reader);
    }

    public String formatCooldown(long remainingMillis) {
        return player.getCooldown().format(durationFormat.format(remainingMillis));
    }

    public String formatLoreCooldown(long remainingMillis) {
        return menu.getLoreCooldown().format(durationFormat.format(remainingMillis));
    }
}
