package org.nezxenka.StrictKits.kit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KitStorage {

    private final File folder;
    private final Logger logger;

    public KitStorage(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("Не удалось создать папку китов " + folder.getPath());
        }
    }

    public List<Kit> loadAll() {
        List<Kit> kits = new ArrayList<>();
        File[] files = folder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return kits;
        }
        for (File file : files) {
            try {
                Kit kit = read(file);
                if (kit == null) {
                    logger.warning("Пропущен кит с недопустимым именем: " + file.getName());
                    continue;
                }
                kits.add(kit);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Не удалось загрузить кит из " + file.getName(), e);
            }
        }
        return kits;
    }

    private Kit read(File file) {
        String fileName = file.getName();
        String fallback = fileName.substring(0, fileName.length() - 4);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = config.getString("Name", fallback);
        if (!Kit.isValidName(name)) {
            name = fallback;
        }
        if (!Kit.isValidName(name)) {
            return null;
        }
        Kit kit = new Kit(name);
        kit.setCooldown(config.getLong("Cooldown", 0L));
        kit.setPermission(config.getString("Permission", "strictkits.kits." + name));
        kit.setOneTimeUse(config.getBoolean("OneTimeUse", false));
        kit.setFirstTimeJoinKit(config.getBoolean("FirstTimeJoinKit", false));
        kit.setIcon(config.getItemStack("Icon"));
        kit.setMainContent(readItems(config, "Inventory.Main"));
        kit.setArmorContent(readItems(config, "Inventory.Armor"));
        kit.consumeDirty();
        return kit;
    }

    private ItemStack[] readItems(YamlConfiguration config, String path) {
        List<?> raw = config.getList(path);
        if (raw == null || raw.isEmpty()) {
            return new ItemStack[0];
        }
        ItemStack[] items = new ItemStack[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            Object value = raw.get(i);
            items[i] = value instanceof ItemStack ? (ItemStack) value : null;
        }
        return items;
    }

    public File fileOf(Kit kit) {
        return new File(folder, kit.getName() + ".yml");
    }

    public void save(Kit kit) {
        if (!Kit.isValidName(kit.getName())) {
            logger.warning("Отказ сохранять кит с недопустимым именем: " + kit.getName());
            return;
        }
        File file = fileOf(kit);
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
            config.save(file);
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
}
