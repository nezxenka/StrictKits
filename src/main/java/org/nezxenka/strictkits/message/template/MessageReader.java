package org.nezxenka.strictkits.message.template;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.nezxenka.strictkits.message.format.Text;

import java.util.List;

public final class MessageReader {

    private final FileConfiguration config;
    private final String prefix;

    public MessageReader(FileConfiguration config, String prefixPath) {
        this.config = config;
        this.prefix = raw(prefixPath);
    }

    public String raw(String path) {
        String value = config.getString(path);
        return value == null ? "" : value;
    }

    public String plain(String path) {
        return Text.color(config.getString(path));
    }

    public String prefixed(String path) {
        String value = raw(path);
        return value.isEmpty() ? "" : Text.color(prefix + value);
    }

    public Message template(String path, String... keys) {
        return Message.compile(config.getString(path), keys);
    }

    public Message prefixedTemplate(String path, String... keys) {
        return withPrefix(raw(path), keys);
    }

    public Message withPrefix(String value, String... keys) {
        return Message.compile(value.isEmpty() ? value : prefix + value, keys);
    }

    public String[] lines(String path) {
        return config.getStringList(path).stream().map(Text::color).toArray(String[]::new);
    }

    public List<String> rawLines(String path) {
        return config.getStringList(path);
    }

    public ConfigurationSection sectionOrDefault(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null && !section.getKeys(false).isEmpty()) {
            return section;
        }
        Configuration defaults = config.getDefaults();
        return defaults == null ? null : defaults.getConfigurationSection(path);
    }
}
