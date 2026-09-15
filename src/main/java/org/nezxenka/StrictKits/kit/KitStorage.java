package org.nezxenka.StrictKits.kit;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class KitStorage {

    private static final String EXTENSION = ".yml";

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
        if (!Kit.isValidName(kit.getName())) {
            logger.warning("Отказ сохранять кит с недопустимым именем: " + kit.getName());
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("Name", kit.getName());
        config.set("Cooldown", kit.getCooldown());
        config.set("Permission", kit.getPermission());
        config.set("OneTimeUse", kit.isOneTimeUse());
        config.set("FirstTimeJoinKit", kit.isFirstTimeJoinKit());
        config.set("Icon", kit.getIcon());
        config.set("Inventory.Main", kit.getMainContent());
        config.set("Inventory.Armor", kit.getArmorContent());
        try {
            config.save(fileOf(kit));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Не удалось сохранить кит " + kit.getName(), e);
        }
    }

    public void delete(Kit kit) {
        if (!Kit.isValidName(kit.getName())) {
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
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String stored = config.getString("Name");
        String name = Kit.isValidName(stored) ? stored : fallback;
        if (!Kit.isValidName(name)) {
            return null;
        }
        Kit kit = new Kit(name);
        kit.setCooldown(config.getLong("Cooldown"));
        kit.setPermission(config.getString("Permission", kit.getPermission()));
        kit.setOneTimeUse(config.getBoolean("OneTimeUse"));
        kit.setFirstTimeJoinKit(config.getBoolean("FirstTimeJoinKit"));
        kit.setIcon(config.getItemStack("Icon"));
        kit.setMainContent(readItems(config, "Inventory.Main"));
        kit.setArmorContent(readItems(config, "Inventory.Armor"));
        kit.consumeDirty();
        return kit;
    }

    private static ItemStack[] readItems(YamlConfiguration config, String path) {
        return config.getList(path, List.of()).stream()
                .map(value -> value instanceof ItemStack item ? item : null)
                .toArray(ItemStack[]::new);
    }

    private File fileOf(Kit kit) {
        return new File(folder, kit.getName() + EXTENSION);
    }
}
