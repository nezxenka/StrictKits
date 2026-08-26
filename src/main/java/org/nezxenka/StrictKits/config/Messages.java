package org.nezxenka.StrictKits.config;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.util.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Messages {

    private static final String[] NO_LINES = new String[0];

    private static final String KIT = ":kit:";
    private static final String PLAYER = ":player:";
    private static final String COOLDOWN = ":cooldown:";
    private static final String PAGE = ":page:";
    private static final String PAGES = ":pages:";
    private static final String VERSION = ":version:";
    private static final String KITS = ":kits:";
    private static final String SECONDS = ":seconds:";
    private static final String FLAG = ":flag:";
    private static final String VALUE = ":value:";
    private static final String PERMISSION = ":permission:";
    private static final String USAGE = ":usage:";
    private static final String STORAGE = ":storage:";
    private static final String CACHE = ":cache:";
    private static final String LOADED = ":loaded:";
    private static final String HITS = ":hits:";
    private static final String LOOKUPS = ":lookups:";
    private static final String RATIO = ":ratio:";
    private static final String WRITES = ":writes:";

    private final String[] playerHelp;
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

    private final Message guiTitle;
    private final Message guiPreviewTitle;
    private final String loreAvailable;
    private final Message loreCooldown;
    private final String loreClaimed;
    private final String loreNoPermission;
    private final Message defaultIconName;
    private final String exitButton;
    private final String previousButton;
    private final String nextButton;

    private final String[] adminHelp;
    private final Message adminUsage;
    private final Map<String, String> adminSyntax;
    private final String adminKitNotFound;
    private final String adminKitExists;
    private final String adminKitNameInvalid;
    private final Message adminKitCreated;
    private final Message adminKitRemoved;
    private final Message adminKitGiven;
    private final Message adminInventoryUpdated;
    private final String adminCooldownNotANumber;
    private final String adminCooldownNegative;
    private final Message adminCooldownTooLarge;
    private final Message adminCooldownUpdated;
    private final Message adminFlagUpdated;
    private final String adminIconHandEmpty;
    private final String adminIconWithoutName;
    private final Message adminIconAlreadyUsed;
    private final Message adminIconUpdated;
    private final Message adminPermissionUpdated;
    private final String adminPurgeNothing;
    private final Message adminPurgeStarted;
    private final Message adminReloaded;
    private final String adminVersion;
    private final Message[] adminStats;

    public Messages(FileConfiguration config, String version) {
        String prefix = raw(config, "admin.prefix");

        this.playerHelp = lines(config, "player.help");
        this.noAccess = plain(config, "player.no-access");
        this.noKitsOnServer = plain(config, "player.no-kits-on-server");
        this.noPermission = plain(config, "player.no-permission");
        this.playersOnly = plain(config, "player.players-only");
        this.playerOffline = plain(config, "player.player-offline");
        this.dataNotLoaded = plain(config, "player.data-not-loaded");
        this.throttled = plain(config, "player.throttled");
        this.kitNotFound = plain(config, "player.kit-not-found");
        this.kitEmpty = plain(config, "player.kit-empty");
        this.kitAlreadyClaimed = plain(config, "player.kit-already-claimed");
        this.previewUsage = plain(config, "player.preview-usage");
        this.kitReceived = template(config, "player.kit-received", KIT);
        this.cooldown = template(config, "player.cooldown", COOLDOWN);
        this.listPrefix = plain(config, "player.list.prefix");
        this.listSeparator = plain(config, "player.list.separator");
        this.listEntry = template(config, "player.list.entry", KIT);
        this.listEntryReady = template(config, "player.list.entry-ready", KIT);
        this.listEntryCooldown = template(config, "player.list.entry-cooldown", KIT);

        this.guiTitle = template(config, "gui.title", PAGE, PAGES);
        this.guiPreviewTitle = template(config, "gui.preview-title", KIT);
        this.loreAvailable = plain(config, "gui.lore-available");
        this.loreCooldown = template(config, "gui.lore-cooldown", COOLDOWN);
        this.loreClaimed = plain(config, "gui.lore-claimed");
        this.loreNoPermission = plain(config, "gui.lore-no-permission");
        this.defaultIconName = template(config, "gui.default-icon-name", KIT);
        this.exitButton = plain(config, "gui.buttons.exit");
        this.previousButton = plain(config, "gui.buttons.previous");
        this.nextButton = plain(config, "gui.buttons.next");

        this.adminHelp = bake(lines(config, "admin.help"), version);
        this.adminUsage = compile(prefix, raw(config, "admin.usage"), USAGE);
        this.adminSyntax = readSyntax(config, adminUsage);
        this.adminKitNotFound = prefixed(config, prefix, "admin.kit-not-found");
        this.adminKitExists = prefixed(config, prefix, "admin.kit-already-exists");
        this.adminKitNameInvalid = prefixed(config, prefix, "admin.kit-name-invalid");
        this.adminKitCreated = adminTemplate(config, prefix, "admin.kit-created", KIT);
        this.adminKitRemoved = adminTemplate(config, prefix, "admin.kit-removed", KIT);
        this.adminKitGiven = adminTemplate(config, prefix, "admin.kit-given", KIT, PLAYER);
        this.adminInventoryUpdated = adminTemplate(config, prefix, "admin.inventory-updated", KIT);
        this.adminCooldownNotANumber = prefixed(config, prefix, "admin.cooldown-not-a-number");
        this.adminCooldownNegative = prefixed(config, prefix, "admin.cooldown-negative");
        this.adminCooldownTooLarge = adminTemplate(config, prefix, "admin.cooldown-too-large", SECONDS);
        this.adminCooldownUpdated = adminTemplate(config, prefix, "admin.cooldown-updated", KIT, SECONDS);
        this.adminFlagUpdated = adminTemplate(config, prefix, "admin.flag-updated", FLAG, KIT, VALUE);
        this.adminIconHandEmpty = prefixed(config, prefix, "admin.icon-hand-empty");
        this.adminIconWithoutName = prefixed(config, prefix, "admin.icon-without-name");
        this.adminIconAlreadyUsed = adminTemplate(config, prefix, "admin.icon-already-used", KIT);
        this.adminIconUpdated = adminTemplate(config, prefix, "admin.icon-updated", KIT);
        this.adminPermissionUpdated = adminTemplate(config, prefix, "admin.permission-updated", KIT, PERMISSION);
        this.adminPurgeNothing = prefixed(config, prefix, "admin.purge-nothing-to-do");
        this.adminPurgeStarted = adminTemplate(config, prefix, "admin.purge-started", KIT);
        this.adminReloaded = adminTemplate(config, prefix, "admin.reloaded", KITS);
        this.adminVersion = adminTemplate(config, prefix, "admin.version", VERSION).format(version);

        String[] stats = lines(config, "admin.stats", false);
        this.adminStats = new Message[stats.length];
        for (int i = 0; i < stats.length; i++) {
            this.adminStats[i] = compile(prefix, stats[i], STORAGE, CACHE, KITS, LOADED, HITS, LOOKUPS, RATIO, WRITES);
        }
    }

    private static String raw(FileConfiguration config, String path) {
        String value = config.getString(path);
        return value == null ? "" : value;
    }

    private static String plain(FileConfiguration config, String path) {
        return Text.color(config.getString(path));
    }

    private static String prefixed(FileConfiguration config, String prefix, String path) {
        String value = raw(config, path);
        return value.isEmpty() ? "" : Text.color(prefix + value);
    }

    private static Message template(FileConfiguration config, String path, String... keys) {
        return Message.compile(config.getString(path), keys);
    }

    private static Message adminTemplate(FileConfiguration config, String prefix, String path, String... keys) {
        return compile(prefix, raw(config, path), keys);
    }

    private static Message compile(String prefix, String value, String... keys) {
        return value.isEmpty() ? Message.compile(value, keys) : Message.compile(prefix + value, keys);
    }

    private static String[] lines(FileConfiguration config, String path) {
        return lines(config, path, true);
    }

    private static String[] lines(FileConfiguration config, String path, boolean colored) {
        List<String> raw = config.getStringList(path);
        if (raw.isEmpty()) {
            return NO_LINES;
        }
        String[] out = new String[raw.size()];
        for (int i = 0; i < out.length; i++) {
            String line = raw.get(i);
            out[i] = colored ? Text.color(line) : line;
        }
        return out;
    }

    private static String[] bake(String[] source, String version) {
        for (int i = 0; i < source.length; i++) {
            if (source[i].indexOf(VERSION) >= 0) {
                source[i] = Message.compile(source[i], VERSION).format(version);
            }
        }
        return source;
    }

    private static Map<String, String> readSyntax(FileConfiguration config, Message usage) {
        ConfigurationSection section = config.getConfigurationSection("admin.syntax");
        if (section == null || section.getKeys(false).isEmpty()) {
            Configuration fallback = config.getDefaults();
            section = fallback == null ? null : fallback.getConfigurationSection("admin.syntax");
        }
        if (section == null) {
            return new HashMap<>(0);
        }
        Set<String> keys = section.getKeys(false);
        Map<String, String> out = new HashMap<>(keys.size() * 2);
        for (String key : keys) {
            String syntax = section.getString(key);
            if (syntax != null && !syntax.isEmpty()) {
                out.put(key.toLowerCase(), usage.format(Text.color(syntax)));
            }
        }
        return out;
    }

    public String[] getPlayerHelp() {
        return playerHelp;
    }

    public String getNoAccess() {
        return noAccess;
    }

    public String getNoKitsOnServer() {
        return noKitsOnServer;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getPlayersOnly() {
        return playersOnly;
    }

    public String getPlayerOffline() {
        return playerOffline;
    }

    public String getDataNotLoaded() {
        return dataNotLoaded;
    }

    public String getThrottled() {
        return throttled;
    }

    public String getKitNotFound() {
        return kitNotFound;
    }

    public String getKitEmpty() {
        return kitEmpty;
    }

    public String getKitAlreadyClaimed() {
        return kitAlreadyClaimed;
    }

    public String getPreviewUsage() {
        return previewUsage;
    }

    public String getKitReceived(String kit) {
        return kitReceived.format(kit);
    }

    public String getCooldown(String formatted) {
        return cooldown.format(formatted);
    }

    public String getListPrefix() {
        return listPrefix;
    }

    public String getListSeparator() {
        return listSeparator;
    }

    public String getListEntry(String kit) {
        return listEntry.format(kit);
    }

    public String getListEntry(String kit, boolean ready) {
        return ready ? listEntryReady.format(kit) : listEntryCooldown.format(kit);
    }

    public String getGuiTitle(int page, int pages) {
        return guiTitle.format(Integer.toString(page), Integer.toString(pages));
    }

    public String getGuiPreviewTitle(String kit) {
        return guiPreviewTitle.format(kit);
    }

    public String getLoreAvailable() {
        return loreAvailable;
    }

    public String getLoreCooldown(String formatted) {
        return loreCooldown.format(formatted);
    }

    public String getLoreClaimed() {
        return loreClaimed;
    }

    public String getDefaultIconName(String kit) {
        return defaultIconName.format(kit);
    }

    public String getLoreNoPermission() {
        return loreNoPermission;
    }

    public String getExitButton() {
        return exitButton;
    }

    public String getPreviousButton() {
        return previousButton;
    }

    public String getNextButton() {
        return nextButton;
    }

    public String[] getAdminHelp() {
        return adminHelp;
    }

    public String getUsage(String subcommand) {
        String cached = adminSyntax.get(subcommand);
        return cached != null ? cached : adminUsage.format("/sk " + subcommand);
    }

    public String getAdminKitNotFound() {
        return adminKitNotFound;
    }

    public String getAdminKitExists() {
        return adminKitExists;
    }

    public String getAdminKitNameInvalid() {
        return adminKitNameInvalid;
    }

    public String getAdminKitCreated(String kit) {
        return adminKitCreated.format(kit);
    }

    public String getAdminKitRemoved(String kit) {
        return adminKitRemoved.format(kit);
    }

    public String getAdminKitGiven(String kit, String player) {
        return adminKitGiven.format(kit, player);
    }

    public String getAdminInventoryUpdated(String kit) {
        return adminInventoryUpdated.format(kit);
    }

    public String getAdminCooldownNotANumber() {
        return adminCooldownNotANumber;
    }

    public String getAdminCooldownNegative() {
        return adminCooldownNegative;
    }

    public String getAdminCooldownTooLarge(long seconds) {
        return adminCooldownTooLarge.format(Long.toString(seconds));
    }

    public String getAdminCooldownUpdated(String kit, long seconds) {
        return adminCooldownUpdated.format(kit, Long.toString(seconds));
    }

    public String getAdminFlagUpdated(String flag, String kit, boolean value) {
        return adminFlagUpdated.format(flag, kit, Boolean.toString(value));
    }

    public String getAdminIconHandEmpty() {
        return adminIconHandEmpty;
    }

    public String getAdminIconWithoutName() {
        return adminIconWithoutName;
    }

    public String getAdminIconAlreadyUsed(String kit) {
        return adminIconAlreadyUsed.format(kit);
    }

    public String getAdminIconUpdated(String kit) {
        return adminIconUpdated.format(kit);
    }

    public String getAdminPermissionUpdated(String kit, String permission) {
        return adminPermissionUpdated.format(kit, permission);
    }

    public String getAdminPurgeNothing() {
        return adminPurgeNothing;
    }

    public String getAdminPurgeStarted(String kit) {
        return adminPurgeStarted.format(kit);
    }

    public String getAdminReloaded(int kits) {
        return adminReloaded.format(Integer.toString(kits));
    }

    public String getAdminVersion() {
        return adminVersion;
    }

    public String[] getAdminStats(String storage, String cache, int kits, PlayerDataManager players) {
        String[] values = {
                storage,
                cache,
                Integer.toString(kits),
                Integer.toString(players.getLoadedCount()),
                Long.toString(players.getCacheHits()),
                Long.toString(players.getCacheLookups()),
                Long.toString(players.getCacheHitRatio()),
                Long.toString(players.getWrites())};
        String[] out = new String[adminStats.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = adminStats[i].format(values);
        }
        return out;
    }
}
