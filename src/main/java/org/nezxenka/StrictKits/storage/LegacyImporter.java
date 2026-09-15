package org.nezxenka.StrictKits.storage;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class LegacyImporter {

    private static final String COOLDOWNS_FILE = "Cooldowns.yml";
    private static final String CLAIMS_FILE = "OneTimeUseList.yml";

    private final File folder;
    private final StorageProvider storage;
    private final Logger logger;
    private final Map<String, UUID> knownPlayers;

    private int skipped;

    public static boolean hasLegacyData(File folder) {
        return new File(folder, COOLDOWNS_FILE).exists() || new File(folder, CLAIMS_FILE).exists();
    }

    public void run() {
        skipped = 0;
        int imported = importCooldowns() + importClaims();
        if (imported > 0) {
            logger.info("Импортировано записей из YAML в базу: " + imported);
        }
        if (skipped > 0) {
            logger.warning("Пропущено записей с неизвестными игроками: " + skipped);
        }
    }

    private int importCooldowns() {
        File file = new File(folder, COOLDOWNS_FILE);
        if (!file.exists()) {
            return 0;
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("Cooldowns");
        List<DataEntry> entries = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int split = key.indexOf('*');
                if (split <= 0 || split == key.length() - 1) {
                    continue;
                }
                UUID uuid = resolve(key.substring(0, split));
                if (uuid != null) {
                    entries.add(new DataEntry(uuid, key.substring(split + 1).toLowerCase(), section.getLong(key)));
                }
            }
        }
        return write(file, entries, storage::writeCooldowns);
    }

    private int importClaims() {
        File file = new File(folder, CLAIMS_FILE);
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
        return write(file, entries, storage::writeClaims);
    }

    private int write(File file, List<DataEntry> entries, BatchWriter writer) {
        try {
            if (!entries.isEmpty()) {
                writer.write(entries);
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

    @FunctionalInterface
    private interface BatchWriter {

        void write(List<DataEntry> entries) throws Exception;
    }
}
