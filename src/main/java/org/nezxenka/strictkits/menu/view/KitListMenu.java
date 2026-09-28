package org.nezxenka.strictkits.menu.view;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.config.settings.MenuSettings;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.menu.holder.KitListHolder;
import org.nezxenka.strictkits.menu.layout.PageLayout;
import org.nezxenka.strictkits.menu.navigation.NavigationButtons;
import org.nezxenka.strictkits.menu.render.KitIconRenderer;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.player.data.PlayerData;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;

import java.util.List;

@RequiredArgsConstructor
public final class KitListMenu {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final PlayerDataManager players;
    private final KitIconRenderer renderer;

    public void open(Player player, int requestedPage) {
        MenuSettings settings = configs.getSettings().getMenu();
        PlayerMessages text = configs.getMessages().getPlayer();
        List<Kit> visible = kits.all().stream()
                .filter(kit -> settings.isShowWithoutPermission() || kit.hasAccess(player))
                .toList();
        if (visible.isEmpty()) {
            Messenger.send(player, kits.size() == 0 ? text.getNoKitsOnServer() : text.getNoAccess());
            return;
        }
        PlayerData data = players.getOrRequest(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, text.getDataNotLoaded());
            return;
        }

        PageLayout layout = PageLayout.of(visible.size(), settings.getRows(), requestedPage);
        Kit[] slots = new Kit[layout.getSize()];
        KitListHolder holder = new KitListHolder(layout, slots);
        Inventory inventory = Bukkit.createInventory(holder, layout.getSize(),
                configs.getMessages().getMenu().getTitle().format(layout.getPage(), layout.getTotalPages()));
        holder.setInventory(inventory);

        int offset = layout.offset();
        int count = layout.itemsOnPage(visible.size());
        for (int slot = 0; slot < count; slot++) {
            Kit kit = visible.get(offset + slot);
            slots[slot] = kit;
            inventory.setItem(slot, renderer.render(player, data, kit));
        }
        NavigationButtons.place(inventory, layout, configs.getMenuItems());
        player.openInventory(inventory);
    }

    public void refresh(Player player, KitListHolder holder) {
        PlayerData data = players.get(player.getUniqueId());
        Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            Kit kit = holder.kitAt(slot);
            if (kit != null) {
                inventory.setItem(slot, renderer.render(player, data, kit));
            }
        }
    }
}
