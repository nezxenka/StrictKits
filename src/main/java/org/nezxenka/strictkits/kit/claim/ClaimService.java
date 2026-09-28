package org.nezxenka.strictkits.kit.claim;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.permission.Permissions;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.kit.delivery.KitDelivery;
import org.nezxenka.strictkits.player.data.PlayerData;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;

@RequiredArgsConstructor
public final class ClaimService {

    private final KitRegistry kits;
    private final PlayerDataManager players;
    private final KitDelivery delivery;

    public ClaimResult claim(Player player, Kit kit) {
        PlayerData data = players.getOrRequest(player.getUniqueId());
        if (data == null) {
            return ClaimResult.of(ClaimResult.Status.DATA_NOT_LOADED);
        }
        boolean admin = player.hasPermission(Permissions.ADMIN);
        if (!admin) {
            ClaimResult check = checkRules(player, data, kit);
            if (!check.isGranted()) {
                return check;
            }
        }
        if (kit.isEmpty()) {
            return ClaimResult.of(ClaimResult.Status.EMPTY);
        }
        delivery.deliver(player, kit);
        if (!admin) {
            record(data, kit, System.currentTimeMillis());
        }
        return ClaimResult.granted();
    }

    public void grant(Player player, Kit kit) {
        delivery.deliver(player, kit);
    }

    public void giveFirstJoinKits(Player player) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Kit kit : kits.all()) {
            if (!kit.isFirstTimeJoinKit() || kit.isEmpty() || (kit.isOneTimeUse() && data.hasClaim(kit.getKey()))) {
                continue;
            }
            delivery.deliver(player, kit);
            record(data, kit, now);
        }
    }

    private static ClaimResult checkRules(Player player, PlayerData data, Kit kit) {
        if (!kit.hasAccess(player)) {
            return ClaimResult.of(ClaimResult.Status.NO_PERMISSION);
        }
        if (kit.isOneTimeUse()) {
            return data.hasClaim(kit.getKey())
                    ? ClaimResult.of(ClaimResult.Status.ALREADY_CLAIMED)
                    : ClaimResult.granted();
        }
        long remaining = ClaimRules.remainingCooldown(data, kit);
        return remaining > 0L ? ClaimResult.onCooldown(remaining) : ClaimResult.granted();
    }

    private void record(PlayerData data, Kit kit, long now) {
        if (kit.isOneTimeUse()) {
            players.recordClaim(data, kit.getKey(), now);
        } else {
            players.recordCooldown(data, kit.getKey(), now);
        }
    }
}
