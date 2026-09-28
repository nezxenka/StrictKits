package org.nezxenka.strictkits.bootstrap;

import lombok.RequiredArgsConstructor;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.menu.holder.MenuHolders;
import org.nezxenka.strictkits.menu.task.MenuRefreshTask;

import java.util.List;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class PluginReloader {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final MenuRefreshTask menuRefresh;
    private final Logger logger;

    public void loadKits() {
        kits.loadAll();
        List<String> withoutIcon = kits.namesWithoutIcon();
        if (!withoutIcon.isEmpty()) {
            logger.warning("Без иконки (используется стандартная, задайте через /sk seticon): " + String.join(", ", withoutIcon));
        }
    }

    public int reload() {
        MenuHolders.closeAll();
        configs.reload();
        kits.awaitPendingWrites();
        loadKits();
        menuRefresh.start(configs.getSettings().getMenu().getRefreshTicks());
        return kits.size();
    }
}
