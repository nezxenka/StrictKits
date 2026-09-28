package org.nezxenka.strictkits.config.loader;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class YamlFiles {

    private final JavaPlugin plugin;

    public void saveIfMissing(String name) {
        if (!file(name).exists()) {
            plugin.saveResource(name, false);
        }
    }

    public YamlConfiguration load(String name, boolean completeMissingKeys) {
        File file = file(name);
        YamlConfiguration config = new YamlConfiguration();
        boolean readable = readInto(config, file);
        Configuration defaults = jarDefaults(name);
        config.setDefaults(defaults);
        if (completeMissingKeys && readable && hasMissingKeys(config, defaults)) {
            config.options().copyDefaults(true);
            try {
                config.save(file);
                plugin.getLogger().info(name + " дополнен новыми ключами");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось обновить " + name, e);
            }
        }
        return config;
    }

    private File file(String name) {
        return new File(plugin.getDataFolder(), name);
    }

    private boolean readInto(YamlConfiguration config, File file) {
        if (!file.exists()) {
            return true;
        }
        try {
            config.load(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось прочитать " + file.getName() + ", применяются значения по умолчанию", e);
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().severe("Синтаксическая ошибка в " + file.getName() + ", применяются значения по умолчанию");
            plugin.getLogger().severe(e.getMessage());
        }
        return false;
    }

    private Configuration jarDefaults(String name) {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(plugin.getResource(name), name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean hasMissingKeys(YamlConfiguration config, Configuration defaults) {
        return defaults.getKeys(true).stream().anyMatch(key -> !config.contains(key, true));
    }
}
