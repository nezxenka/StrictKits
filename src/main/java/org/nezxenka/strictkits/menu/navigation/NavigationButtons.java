package org.nezxenka.strictkits.menu.navigation;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.Inventory;
import org.nezxenka.strictkits.config.menu.MenuItems;
import org.nezxenka.strictkits.menu.layout.PageLayout;

@UtilityClass
public class NavigationButtons {

    public static void place(Inventory inventory, PageLayout layout, MenuItems items) {
        if (!layout.isPaged()) {
            return;
        }
        if (layout.hasPrevious()) {
            inventory.setItem(layout.previousSlot(), items.getPreviousButton());
        }
        if (layout.hasNext()) {
            inventory.setItem(layout.nextSlot(), items.getNextButton());
        }
        inventory.setItem(layout.exitSlot(), items.getExitButton());
    }
}
