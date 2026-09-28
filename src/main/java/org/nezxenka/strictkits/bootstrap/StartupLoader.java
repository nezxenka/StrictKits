package org.nezxenka.strictkits.bootstrap;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.config.database.DatabaseSettings;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;
import org.nezxenka.strictkits.storage.api.StorageProvider;
import org.nezxenka.strictkits.storage.migration.LegacyImporter;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class StartupLoader {

    private static final String LEGACY_FOLDER = "Cooldowns";

    private final File dataFolder;
    private final DatabaseSettings database;
    private final StorageProvider storage;
    private final PlayerDataManager players;
    private final Logger logger;

    public void run() {
        File legacyFolder = new File(dataFolder, LEGACY_FOLDER);
        if (database.isImportLegacyYaml() && LegacyImporter.hasLegacyData(legacyFolder)) {
            players.runAsync(new LegacyImporter(legacyFolder, storage, logger, knownPlayerNames())::run);
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            players.markOnline(online.getUniqueId());
            players.requestLoad(online.getUniqueId());
        }
    }

    private static Map<String, UUID> knownPlayerNames() {
        Map<String, UUID> known = new HashMap<>();
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String name = offline.getName();
            if (name != null) {
                known.put(name.toLowerCase(Locale.ROOT), offline.getUniqueId());
            }
        }
        return known;
    }
}
