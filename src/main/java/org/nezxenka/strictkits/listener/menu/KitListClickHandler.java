package org.nezxenka.strictkits.listener.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.claim.ClaimFeedback;
import org.nezxenka.strictkits.kit.claim.ClaimResult;
import org.nezxenka.strictkits.kit.claim.ClaimService;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.menu.holder.KitListHolder;
import org.nezxenka.strictkits.menu.holder.MenuHolders;
import org.nezxenka.strictkits.menu.layout.PageLayout;
import org.nezxenka.strictkits.menu.view.KitListMenu;
import org.nezxenka.strictkits.menu.view.KitPreviewMenu;
import org.nezxenka.strictkits.util.scheduler.PlayerTasks;

@RequiredArgsConstructor
public final class KitListClickHandler {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final ClaimService claims;
    private final ClaimFeedback feedback;
    private final KitListMenu listMenu;
    private final KitPreviewMenu previewMenu;
    private final PlayerTasks tasks;

    public void handle(Player player, KitListHolder holder, int slot, boolean rightClick) {
        PageLayout layout = holder.getLayout();
        if (slot == layout.previousSlot() && layout.hasPrevious()) {
            reopen(player, layout.getPage() - 1);
            return;
        }
        if (slot == layout.nextSlot() && layout.hasNext()) {
            reopen(player, layout.getPage() + 1);
            return;
        }
        Kit kit = holder.kitAt(slot);
        if (kit == null) {
            return;
        }
        if (!kits.contains(kit)) {
            tasks.runLater(player, online -> {
                MenuHolders.close(online);
                listMenu.open(online, layout.getPage());
            });
            return;
        }
        if (rightClick && configs.getSettings().getMenu().isPreviewOnRightClick()) {
            tasks.runLater(player, online -> previewMenu.open(online, kit));
            return;
        }
        ClaimResult result = claims.claim(player, kit);
        feedback.send(player, kit, result);
        if (result.isGranted()) {
            reopen(player, layout.getPage());
        }
    }

    private void reopen(Player player, int page) {
        tasks.runLater(player, online -> listMenu.open(online, page));
    }
}
