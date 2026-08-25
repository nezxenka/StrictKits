package org.nezxenka.StrictKits.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.nezxenka.StrictKits.util.Text;

import java.util.List;

public final class Lang {

    private final String noAccess;
    private final List<String> listHelp;
    private final String previewUsageError;
    private final String noKitServer;
    private final String noPermission;
    private final String alreadyGotOneTimeUseKit;
    private final String kitDoesntExist;
    private final String receivedKit;
    private final String cooldownMessage;
    private final String kitListPrefix;
    private final String kitListSeparator;
    private final String emptyKit;
    private final String dataNotLoaded;
    private final String throttled;
    private final String playerOffline;
    private final String playersOnly;

    private final String guiTitle;
    private final String guiPreviewTitle;
    private final String loreAvailable;
    private final String loreCooldown;
    private final String loreClaimed;
    private final String loreNoPermission;

    public Lang(FileConfiguration config) {
        this.noAccess = read(config, "lang.no-access", "&eДля вас нету доступных китов");
        this.listHelp = Text.color(config.getStringList("lang.list-help"));
        this.previewUsageError = read(config, "lang.preview-usage-error", "&cИспользуй: /kit preview <name>");
        this.noKitServer = read(config, "lang.no-kit-server", "&cНа сервер нету ни одного кита");
        this.noPermission = read(config, "lang.no-permission", "&cУ вас нету права на использование команды");
        this.alreadyGotOneTimeUseKit = read(config, "lang.already-got-one-time-use-kit", "&cВы уже получали этот кит");
        this.kitDoesntExist = read(config, "lang.kit-doesnt-exist", "&cКита не существует");
        this.receivedKit = read(config, "lang.received-kit", "&aВы успешно получили кит&e :kitname:");
        this.cooldownMessage = read(config, "lang.cooldown-message", "&cВы сможете получить этот кит через&e :cooldown:");
        this.kitListPrefix = read(config, "lang.kit-list-prefix", "&7Киты: ");
        this.kitListSeparator = read(config, "lang.kit-list-separator", "&7, ");
        this.emptyKit = read(config, "lang.empty-kit", "&cУ этого набора еще нет инвентаря");
        this.dataNotLoaded = read(config, "lang.data-not-loaded", "&cВаши данные ещё загружаются");
        this.throttled = read(config, "lang.throttled", "&cНе так быстро");
        this.playerOffline = read(config, "lang.player-offline", "&cЭтот игрок не существует/не находится в сети");
        this.playersOnly = read(config, "lang.players-only", "&cКоманда доступна только для игроков");

        this.guiTitle = read(config, "gui.title", "&7Киты: :page:/:pages:");
        this.guiPreviewTitle = read(config, "gui.preview-title", "&7Просмотр кита: :kitname:");
        this.loreAvailable = read(config, "gui.lore-available", "&aДоступен");
        this.loreCooldown = read(config, "gui.lore-cooldown", "&cЗадержка: &e:cooldown:");
        this.loreClaimed = read(config, "gui.lore-claimed", "&cУже получен");
        this.loreNoPermission = read(config, "gui.lore-no-permission", "&cНет доступа");
    }

    private static String read(FileConfiguration config, String path, String fallback) {
        return Text.color(config.getString(path, fallback));
    }

    public String getNoAccess() {
        return noAccess;
    }

    public List<String> getListHelp() {
        return listHelp;
    }

    public String getPreviewUsageError() {
        return previewUsageError;
    }

    public String getNoKitServer() {
        return noKitServer;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getAlreadyGotOneTimeUseKit() {
        return alreadyGotOneTimeUseKit;
    }

    public String getKitDoesntExist() {
        return kitDoesntExist;
    }

    public String getReceivedKit(String kitName) {
        return Text.replace(receivedKit, ":kitname:", kitName);
    }

    public String getCooldownMessage(String formatted) {
        return Text.replace(cooldownMessage, ":cooldown:", formatted);
    }

    public String getKitListPrefix() {
        return kitListPrefix;
    }

    public String getKitListSeparator() {
        return kitListSeparator;
    }

    public String getEmptyKit() {
        return emptyKit;
    }

    public String getDataNotLoaded() {
        return dataNotLoaded;
    }

    public String getThrottled() {
        return throttled;
    }

    public String getPlayerOffline() {
        return playerOffline;
    }

    public String getPlayersOnly() {
        return playersOnly;
    }

    public String getGuiTitle(int page, int pages) {
        return Text.replace(Text.replace(guiTitle, ":page:", Integer.toString(page)), ":pages:", Integer.toString(pages));
    }

    public String getGuiPreviewTitle(String kitName) {
        return Text.replace(guiPreviewTitle, ":kitname:", kitName);
    }

    public String getLoreAvailable() {
        return loreAvailable;
    }

    public String getLoreCooldown(String formatted) {
        return Text.replace(loreCooldown, ":cooldown:", formatted);
    }

    public String getLoreClaimed() {
        return loreClaimed;
    }

    public String getLoreNoPermission() {
        return loreNoPermission;
    }
}
