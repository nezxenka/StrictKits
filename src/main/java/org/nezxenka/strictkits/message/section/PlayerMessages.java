package org.nezxenka.strictkits.message.section;

import lombok.Getter;
import org.nezxenka.strictkits.message.template.Message;
import org.nezxenka.strictkits.message.template.MessageReader;
import org.nezxenka.strictkits.message.template.Placeholders;

import java.util.List;

@Getter
public final class PlayerMessages {

    private final String[] help;
    private final String noAccess;
    private final String noKitsOnServer;
    private final String noPermission;
    private final String playersOnly;
    private final String playerOffline;
    private final String dataNotLoaded;
    private final String throttled;
    private final String kitNotFound;
    private final String kitEmpty;
    private final String kitAlreadyClaimed;
    private final String previewUsage;
    private final Message kitReceived;
    private final Message cooldown;
    private final String listPrefix;
    private final String listSeparator;
    private final Message listEntry;
    private final Message listEntryReady;
    private final Message listEntryCooldown;

    public PlayerMessages(MessageReader reader) {
        this.help = reader.lines("player.help");
        this.noAccess = reader.plain("player.no-access");
        this.noKitsOnServer = reader.plain("player.no-kits-on-server");
        this.noPermission = reader.plain("player.no-permission");
        this.playersOnly = reader.plain("player.players-only");
        this.playerOffline = reader.plain("player.player-offline");
        this.dataNotLoaded = reader.plain("player.data-not-loaded");
        this.throttled = reader.plain("player.throttled");
        this.kitNotFound = reader.plain("player.kit-not-found");
        this.kitEmpty = reader.plain("player.kit-empty");
        this.kitAlreadyClaimed = reader.plain("player.kit-already-claimed");
        this.previewUsage = reader.plain("player.preview-usage");
        this.kitReceived = reader.template("player.kit-received", Placeholders.KIT);
        this.cooldown = reader.template("player.cooldown", Placeholders.COOLDOWN);
        this.listPrefix = reader.plain("player.list.prefix");
        this.listSeparator = reader.plain("player.list.separator");
        this.listEntry = reader.template("player.list.entry", Placeholders.KIT);
        this.listEntryReady = reader.template("player.list.entry-ready", Placeholders.KIT);
        this.listEntryCooldown = reader.template("player.list.entry-cooldown", Placeholders.KIT);
    }

    public String formatList(List<String> entries) {
        return listPrefix + String.join(listSeparator, entries);
    }
}
