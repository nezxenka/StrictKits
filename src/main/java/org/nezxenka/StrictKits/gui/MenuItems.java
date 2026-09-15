package org.nezxenka.StrictKits.gui;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.StrictKits.config.Messages;

@Getter
public final class MenuItems {

    private final ItemStack exitButton;
    private final ItemStack previousButton;
    private final ItemStack nextButton;
    private final ItemStack defaultKitIcon;

    public MenuItems(FileConfiguration config, Messages messages) {
        this.exitButton = read(config, "GUItems.ExitButton", Material.STONE_BUTTON, messages.getExitButton());
        this.previousButton = read(config, "GUItems.PreviousButton", Material.ARROW, messages.getPreviousButton());
        this.nextButton = read(config, "GUItems.NextButton", Material.ARROW, messages.getNextButton());
        this.defaultKitIcon = read(config, "GUItems.DefaultKitIcon", Material.CHEST, null);
    }

    public ItemStack createDefaultIcon(String displayName) {
        return named(defaultKitIcon.clone(), displayName);
    }

    private static ItemStack read(FileConfiguration config, String path, Material fallback, String displayName) {
        ItemStack stored = config.getItemStack(path);
        return named(stored == null ? new ItemStack(fallback) : stored.clone(), displayName);
    }

    private static ItemStack named(ItemStack item, String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }
}
