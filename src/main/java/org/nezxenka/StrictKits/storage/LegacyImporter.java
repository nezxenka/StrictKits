package org.nezxenka.StrictKits.storage;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LegacyImporter {

    private final File folder;
    private final StorageProvider storage;
    private final Logger logger;

    public LegacyImporter(File folder, StorageProvider storage, Logger logger) {
        this.folder = folder;
        this.storage = storage;
        this.logger = logger;
    }

    public boolean hasLegacyData() {
        return new File(folder, "Cooldowns.yml").exists() || new File(folder, "OneTimeUseList.yml").exists();
    }

    public void run() {
        Map<String, UUID> resolved = new HashMap<>();
        int imported = 0;
        imported += importCooldowns(resolved);
        imported += importClaims(resolved);
        if (imported > 0) {
            logger.info("Импортировано записей из YAML в базу: " + imported);
        }
    }

    private int importCooldowns(Map<String, UUID> resolved) {
        File file = new File(folder, "Cooldowns.yml");
        if (!file.exists()) {
            return 0;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("Cooldowns");
        if (section == null) {
            archive(file);
            return 0;
        }
        List<DataEntry> entries = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            int split = key.indexOf('*');
            if (split <= 0 || split == key.length() - 1) {
                continue;
            }
            UUID uuid = resolve(resolved, key.substring(0, split));
            if (uuid == null) {
                continue;
            }
            entries.add(new DataEntry(uuid, key.substring(split + 1).toLowerCase(), section.getLong(key)));
        }
        return write(file, entries, true);
    }

    private int importClaims(Map<String, UUID> resolved) {
        File file = new File(folder, "OneTimeUseList.yml");
        if (!file.exists()) {
            return 0;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<DataEntry> entries = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String kit : config.getKeys(false)) {
            for (String name : config.getStringList(kit)) {
                UUID uuid = resolve(resolved, name);
                if (uuid != null) {
                    entries.add(new DataEntry(uuid, kit.toLowerCase(), now));
                }
            }
        }
        return write(file, entries, false);
    }

    private int write(File file, List<DataEntry> entries, boolean cooldowns) {
        if (entries.isEmpty()) {
            archive(file);
            return 0;
        }
        try {
            if (cooldowns) {
                storage.writeCooldowns(entries);
            } else {
                storage.writeClaims(entries);
            }
            archive(file);
            return entries.size();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Не удалось импортировать " + file.getName(), e);
            return 0;
        }
    }

    private UUID resolve(Map<String, UUID> cache, String name) {
        UUID cached = cache.get(name);
        if (cached != null) {
            return cached;
        }
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            UUID uuid = offline.getUniqueId();
            cache.put(name, uuid);
            return uuid;
        } catch (Exception e) {
            return null;
        }
    }

    private void archive(File file) {
        File target = new File(file.getParentFile(), file.getName() + ".migrated");
        if (target.exists()) {
            target.delete();
        }
        if (!file.renameTo(target)) {
            logger.warning("Не удалось переименовать " + file.getName() + " после импорта");
        }
    }
}
