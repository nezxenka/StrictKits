package org.nezxenka.StrictKits.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.util.Text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public final class Messages {

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
    @Getter(AccessLevel.NONE)
    private final Message adminUsage;
    @Getter(AccessLevel.NONE)
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
    private final String adminPermissionInvalid;
    private final Message adminPermissionUpdated;
    private final String adminPurgeNothing;
    private final Message adminPurgeStarted;
    private final Message adminReloaded;
    private final String adminVersion;
    @Getter(AccessLevel.NONE)
    private final Message[] adminStats;

    public Messages(FileConfiguration config, String version) {
        String prefix = raw(config, "admin.prefix");

        this.playerHelp = colorLines(config, "player.help");
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

        this.adminHelp = withVersion(colorLines(config, "admin.help"), version);
        this.adminUsage = prefixedTemplate(prefix, raw(config, "admin.usage"), USAGE);
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
        this.adminPermissionInvalid = prefixed(config, prefix, "admin.permission-invalid");
        this.adminPermissionUpdated = adminTemplate(config, prefix, "admin.permission-updated", KIT, PERMISSION);
        this.adminPurgeNothing = prefixed(config, prefix, "admin.purge-nothing-to-do");
        this.adminPurgeStarted = adminTemplate(config, prefix, "admin.purge-started", KIT);
        this.adminReloaded = adminTemplate(config, prefix, "admin.reloaded", KITS);
        this.adminVersion = adminTemplate(config, prefix, "admin.version", VERSION).format(version);
        this.adminStats = config.getStringList("admin.stats").stream()
                .map(line -> prefixedTemplate(prefix, line, STORAGE, CACHE, KITS, LOADED, HITS, LOOKUPS, RATIO, WRITES))
                .toArray(Message[]::new);
    }

    public String getUsage(String subcommand) {
        String syntax = adminSyntax.get(subcommand);
        return syntax != null ? syntax : adminUsage.format("/sk " + subcommand);
    }

    public String[] formatAdminStats(String storage, String cache, int kits, PlayerDataManager players) {
        Object[] values = {
                storage,
                cache,
                kits,
                players.getLoadedCount(),
                players.getCacheHits(),
                players.getCacheLookups(),
                players.getCacheHitRatio(),
                players.getWrites()};
        return Arrays.stream(adminStats).map(line -> line.format(values)).toArray(String[]::new);
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
        return prefixedTemplate(prefix, raw(config, path), keys);
    }

    private static Message prefixedTemplate(String prefix, String value, String... keys) {
        return Message.compile(value.isEmpty() ? value : prefix + value, keys);
    }

    private static String[] colorLines(FileConfiguration config, String path) {
        return config.getStringList(path).stream().map(Text::color).toArray(String[]::new);
    }

    private static String[] withVersion(String[] lines, String version) {
        return Arrays.stream(lines)
                .map(line -> line.contains(VERSION) ? Message.compile(line, VERSION).format(version) : line)
                .toArray(String[]::new);
    }

    private static Map<String, String> readSyntax(FileConfiguration config, Message usage) {
        ConfigurationSection section = config.getConfigurationSection("admin.syntax");
        if (section == null || section.getKeys(false).isEmpty()) {
            Configuration defaults = config.getDefaults();
            section = defaults == null ? null : defaults.getConfigurationSection("admin.syntax");
        }
        Map<String, String> syntax = new HashMap<>();
        if (section == null) {
            return syntax;
        }
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null && !value.isEmpty()) {
                syntax.put(key.toLowerCase(), usage.format(Text.color(value)));
            }
        }
        return syntax;
    }
}
