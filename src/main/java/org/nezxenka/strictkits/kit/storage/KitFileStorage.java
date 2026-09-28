package org.nezxenka.strictkits.kit.storage;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.naming.KitNames;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class KitFileStorage {

    private static final String EXTENSION = ".yml";
    private static final String TEMP_SUFFIX = ".tmp";

    private final File folder;
    private final Logger logger;

    public List<Kit> loadAll() {
        List<Kit> kits = new ArrayList<>();
        File[] files = folder.listFiles((dir, fileName) -> fileName.endsWith(EXTENSION));
        if (files == null) {
            return kits;
        }
        for (File file : files) {
            try {
                Kit kit = read(file);
                if (kit == null) {
                    logger.warning("Пропущен кит с недопустимым именем: " + file.getName());
                } else {
                    kits.add(kit);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Не удалось загрузить кит из " + file.getName(), e);
            }
        }
        return kits;
    }

    public void save(Kit kit) {
        if (!KitNames.isValid(kit.getName())) {
            logger.warning("Отказ сохранять кит с недопустимым именем: " + kit.getName());
            return;
        }
        File target = fileOf(kit);
        File temp = new File(folder, target.getName() + TEMP_SUFFIX);
        try {
            KitYamlCodec.encode(kit).save(temp);
            replace(temp.toPath(), target.toPath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Не удалось сохранить кит " + kit.getName(), e);
        }
    }

    public void delete(Kit kit) {
        if (!KitNames.isValid(kit.getName())) {
            return;
        }
        File file = fileOf(kit);
        if (file.exists() && !file.delete()) {
            logger.warning("Не удалось удалить файл кита " + file.getName());
        }
    }

    private Kit read(File file) {
        String fileName = file.getName();
        String fallback = fileName.substring(0, fileName.length() - EXTENSION.length());
        return KitYamlCodec.decode(YamlConfiguration.loadConfiguration(file), fallback);
    }

    private File fileOf(Kit kit) {
        return new File(folder, kit.getName() + EXTENSION);
    }

    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
