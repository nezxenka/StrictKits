package org.nezxenka.StrictKits.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GUItems {

    private static ItemStack exitButton;
    private static ItemStack previousButton;
    private static ItemStack nextButton;

    private GUItems() {
    }

    public static void load(FileConfiguration config) {
        exitButton = read(config, "GUItems.ExitButton", Material.STONE_BUTTON, "§cВыход");
        previousButton = read(config, "GUItems.PreviousButton", Material.ARROW, "§eНазад");
        nextButton = read(config, "GUItems.NextButton", Material.ARROW, "§eВперёд");
    }

    private static ItemStack read(FileConfiguration config, String path, Material fallback, String name) {
        ItemStack stored = config.getItemStack(path);
        if (stored != null) {
            return stored;
        }
        ItemStack item = new ItemStack(fallback);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getExitButton() {
        return exitButton;
    }

    public static ItemStack getPreviousButton() {
        return previousButton;
    }

    public static ItemStack getNextButton() {
        return nextButton;
    }
}
