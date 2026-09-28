package org.nezxenka.strictkits.kit.storage;

import lombok.experimental.UtilityClass;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.model.KitContents;
import org.nezxenka.strictkits.kit.naming.KitNames;

import java.util.List;

@UtilityClass
public class KitYamlCodec {

    private static final String NAME = "Name";
    private static final String COOLDOWN = "Cooldown";
    private static final String PERMISSION = "Permission";
    private static final String ONE_TIME_USE = "OneTimeUse";
    private static final String FIRST_TIME_JOIN_KIT = "FirstTimeJoinKit";
    private static final String ICON = "Icon";
    private static final String MAIN_INVENTORY = "Inventory.Main";
    private static final String ARMOR_INVENTORY = "Inventory.Armor";

    public static YamlConfiguration encode(Kit kit) {
        KitContents contents = kit.getContents();
        YamlConfiguration config = new YamlConfiguration();
        config.set(NAME, kit.getName());
        config.set(COOLDOWN, kit.getCooldown());
        config.set(PERMISSION, kit.getPermission());
        config.set(ONE_TIME_USE, kit.isOneTimeUse());
        config.set(FIRST_TIME_JOIN_KIT, kit.isFirstTimeJoinKit());
        config.set(ICON, kit.getIcon());
        config.set(MAIN_INVENTORY, contents.mainItems());
        config.set(ARMOR_INVENTORY, contents.armorItems());
        return config;
    }

    public static Kit decode(YamlConfiguration config, String fallbackName) {
        String stored = config.getString(NAME);
        String name = KitNames.isValid(stored) ? stored : fallbackName;
        if (!KitNames.isValid(name)) {
            return null;
        }
        Kit kit = new Kit(name);
        kit.setCooldown(config.getLong(COOLDOWN));
        kit.setPermission(config.getString(PERMISSION, kit.getPermission()));
        kit.setOneTimeUse(config.getBoolean(ONE_TIME_USE));
        kit.setFirstTimeJoinKit(config.getBoolean(FIRST_TIME_JOIN_KIT));
        kit.setIcon(config.getItemStack(ICON));
        kit.setContents(KitContents.of(readItems(config, MAIN_INVENTORY), readItems(config, ARMOR_INVENTORY)));
        return kit;
    }

    private static ItemStack[] readItems(YamlConfiguration config, String path) {
        return config.getList(path, List.of()).stream()
                .map(value -> value instanceof ItemStack item ? item : null)
                .toArray(ItemStack[]::new);
    }
}
