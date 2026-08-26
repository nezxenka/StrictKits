package org.nezxenka.StrictKits.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LegacyImporter {

    private final File folder;
    private final StorageProvider storage;
    private final Logger logger;
    private final Map<String, UUID> knownPlayers;
    private int skipped;

    public LegacyImporter(File folder, StorageProvider storage, Logger logger, Map<String, UUID> knownPlayers) {
        this.folder = folder;
        this.storage = storage;
        this.logger = logger;
        this.knownPlayers = knownPlayers;
    }

    public boolean hasLegacyData() {
        return new File(folder, "Cooldowns.yml").exists() || new File(folder, "OneTimeUseList.yml").exists();
    }

    public void run() {
        skipped = 0;
        int imported = 0;
        imported += importCooldowns();
        imported += importClaims();
        if (imported > 0) {
            logger.info("Импортировано записей из YAML в базу: " + imported);
        }
        if (skipped > 0) {
            logger.warning("Пропущено записей с неизвестными игроками: " + skipped);
        }
    }

    private int importCooldowns() {
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
            UUID uuid = resolve(key.substring(0, split));
            if (uuid == null) {
                continue;
            }
            entries.add(new DataEntry(uuid, key.substring(split + 1).toLowerCase(), section.getLong(key)));
        }
        return write(file, entries, true);
    }

    private int importClaims() {
        File file = new File(folder, "OneTimeUseList.yml");
        if (!file.exists()) {
            return 0;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<DataEntry> entries = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String kit : config.getKeys(false)) {
            for (String name : config.getStringList(kit)) {
                UUID uuid = resolve(name);
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

    private UUID resolve(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        UUID uuid = knownPlayers.get(name.toLowerCase());
        if (uuid == null) {
            skipped++;
        }
        return uuid;
    }

    private void archive(File file) {
        File target = new File(file.getParentFile(), file.getName() + ".migrated");
        if (target.exists() && !target.delete()) {
            logger.warning("Не удалось удалить старый " + target.getName());
            return;
        }
        if (!file.renameTo(target)) {
            logger.warning("Не удалось переименовать " + file.getName() + " после импорта");
        }
    }
}
