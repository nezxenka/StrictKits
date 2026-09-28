package org.nezxenka.strictkits.menu.view;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.model.KitContents;
import org.nezxenka.strictkits.menu.holder.PreviewHolder;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.permission.Permissions;

@RequiredArgsConstructor
public final class KitPreviewMenu {

    private static final int SIZE = 54;
    private static final int BOOTS_SLOT = 39;
    private static final int EXIT_SLOT = 49;

    private final ConfigManager configs;

    public void open(Player player, Kit kit) {
        PlayerMessages text = configs.getMessages().getPlayer();
        if (configs.getSettings().getAccess().isPreviewRequiresPermission() && !player.hasPermission(Permissions.PREVIEW)) {
            Messenger.send(player, text.getNoPermission());
            return;
        }
        if (kit.isEmpty()) {
            Messenger.send(player, text.getKitEmpty());
            return;
        }
        PreviewHolder holder = new PreviewHolder(EXIT_SLOT);
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                configs.getMessages().getMenu().getPreviewTitle().format(kit.getName()));
        holder.setInventory(inventory);
        fill(inventory, kit.getContents());
        inventory.setItem(EXIT_SLOT, configs.getMenuItems().getExitButton());
        player.openInventory(inventory);
    }

    private static void fill(Inventory inventory, KitContents contents) {
        for (int slot = 0; slot < Math.min(contents.mainSize(), KitContents.STORAGE_SIZE); slot++) {
            inventory.setItem(slot, contents.mainItem(slot));
        }
        for (int index = 0; index < Math.min(contents.armorSize(), KitContents.ARMOR_SIZE); index++) {
            inventory.setItem(BOOTS_SLOT - index, contents.armorItem(index));
        }
        if (contents.mainSize() > KitContents.OFFHAND_SLOT) {
            inventory.setItem(KitContents.OFFHAND_SLOT, contents.mainItem(KitContents.OFFHAND_SLOT));
        }
    }
}
