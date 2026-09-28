package org.nezxenka.strictkits.menu.render;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.kit.claim.ClaimRules;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.message.Messages;
import org.nezxenka.strictkits.message.section.MenuMessages;
import org.nezxenka.strictkits.player.data.PlayerData;

@UtilityClass
public class KitStatusLine {

    public static String resolve(Player player, PlayerData data, Kit kit, Messages messages) {
        MenuMessages text = messages.getMenu();
        if (data == null) {
            return text.getLoreAvailable();
        }
        if (!kit.hasAccess(player)) {
            return text.getLoreNoPermission();
        }
        if (kit.isOneTimeUse()) {
            return data.hasClaim(kit.getKey()) ? text.getLoreClaimed() : text.getLoreAvailable();
        }
        long remaining = ClaimRules.remainingCooldown(data, kit);
        return remaining > 0L ? messages.formatLoreCooldown(remaining) : text.getLoreAvailable();
    }
}
