package org.nezxenka.strictkits.menu.render;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.message.Messages;
import org.nezxenka.strictkits.player.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class KitIconRenderer {

    private final ConfigManager configs;

    public ItemStack render(Player player, PlayerData data, Kit kit) {
        Messages messages = configs.getMessages();
        ItemStack item = kit.hasIcon()
                ? kit.getIcon()
                : configs.getMenuItems().createDefaultIcon(messages.getMenu().getDefaultIconName().format(kit.getName()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>(1);
        lore.add(KitStatusLine.resolve(player, data, kit, messages));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
