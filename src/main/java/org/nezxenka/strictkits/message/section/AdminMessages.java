package org.nezxenka.strictkits.message.section;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.nezxenka.strictkits.message.format.Text;
import org.nezxenka.strictkits.message.template.Message;
import org.nezxenka.strictkits.message.template.MessageReader;
import org.nezxenka.strictkits.message.template.Placeholders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
public final class AdminMessages {

    private static final String COMMAND_PREFIX = "/sk ";

    private final String[] help;
    @Getter(AccessLevel.NONE)
    private final Message usage;
    @Getter(AccessLevel.NONE)
    private final Map<String, String> syntax;
    private final String kitNotFound;
    private final String kitExists;
    private final String kitNameInvalid;
    private final Message kitCreated;
    private final Message kitRemoved;
    private final Message kitGiven;
    private final Message inventoryUpdated;
    private final String cooldownNotANumber;
    private final String cooldownNegative;
    private final Message cooldownTooLarge;
    private final Message cooldownUpdated;
    private final Message flagUpdated;
    private final String iconHandEmpty;
    private final String iconWithoutName;
    private final Message iconUpdated;
    private final String permissionInvalid;
    private final Message permissionUpdated;
    private final String purgeNothing;
    private final Message purgeStarted;
    private final Message reloaded;
    private final String version;
    @Getter(AccessLevel.NONE)
    private final Message[] stats;

    public AdminMessages(MessageReader reader, String pluginVersion) {
        this.help = withVersion(reader.lines("admin.help"), pluginVersion);
        this.usage = reader.prefixedTemplate("admin.usage", Placeholders.USAGE);
        this.syntax = readSyntax(reader.sectionOrDefault("admin.syntax"), usage);
        this.kitNotFound = reader.prefixed("admin.kit-not-found");
        this.kitExists = reader.prefixed("admin.kit-already-exists");
        this.kitNameInvalid = reader.prefixed("admin.kit-name-invalid");
        this.kitCreated = reader.prefixedTemplate("admin.kit-created", Placeholders.KIT);
        this.kitRemoved = reader.prefixedTemplate("admin.kit-removed", Placeholders.KIT);
        this.kitGiven = reader.prefixedTemplate("admin.kit-given", Placeholders.KIT, Placeholders.PLAYER);
        this.inventoryUpdated = reader.prefixedTemplate("admin.inventory-updated", Placeholders.KIT);
        this.cooldownNotANumber = reader.prefixed("admin.cooldown-not-a-number");
        this.cooldownNegative = reader.prefixed("admin.cooldown-negative");
        this.cooldownTooLarge = reader.prefixedTemplate("admin.cooldown-too-large", Placeholders.SECONDS);
        this.cooldownUpdated = reader.prefixedTemplate("admin.cooldown-updated", Placeholders.KIT, Placeholders.SECONDS);
        this.flagUpdated = reader.prefixedTemplate("admin.flag-updated",
                Placeholders.FLAG, Placeholders.KIT, Placeholders.VALUE);
        this.iconHandEmpty = reader.prefixed("admin.icon-hand-empty");
        this.iconWithoutName = reader.prefixed("admin.icon-without-name");
        this.iconUpdated = reader.prefixedTemplate("admin.icon-updated", Placeholders.KIT);
        this.permissionInvalid = reader.prefixed("admin.permission-invalid");
        this.permissionUpdated = reader.prefixedTemplate("admin.permission-updated",
                Placeholders.KIT, Placeholders.PERMISSION);
        this.purgeNothing = reader.prefixed("admin.purge-nothing-to-do");
        this.purgeStarted = reader.prefixedTemplate("admin.purge-started", Placeholders.KIT);
        this.reloaded = reader.prefixedTemplate("admin.reloaded", Placeholders.KITS);
        this.version = reader.prefixedTemplate("admin.version", Placeholders.VERSION).format(pluginVersion);
        this.stats = reader.rawLines("admin.stats").stream()
                .map(line -> reader.withPrefix(line,
                        Placeholders.STORAGE, Placeholders.KITS, Placeholders.LOADED, Placeholders.WRITES))
                .toArray(Message[]::new);
    }

    public String getUsage(String subcommand) {
        String known = syntax.get(subcommand);
        return known != null ? known : usage.format(COMMAND_PREFIX + subcommand);
    }

    public String[] formatStats(String storage, int kits, int loaded, long writes) {
        Object[] values = {storage, kits, loaded, writes};
        return Arrays.stream(stats).map(line -> line.format(values)).toArray(String[]::new);
    }

    private static String[] withVersion(String[] lines, String pluginVersion) {
        return Arrays.stream(lines)
                .map(line -> line.contains(Placeholders.VERSION)
                        ? Message.compile(line, Placeholders.VERSION).format(pluginVersion)
                        : line)
                .toArray(String[]::new);
    }

    private static Map<String, String> readSyntax(ConfigurationSection section, Message usage) {
        Map<String, String> syntax = new HashMap<>();
        if (section == null) {
            return syntax;
        }
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null && !value.isEmpty()) {
                syntax.put(key.toLowerCase(Locale.ROOT), usage.format(Text.color(value)));
            }
        }
        return syntax;
    }
}
