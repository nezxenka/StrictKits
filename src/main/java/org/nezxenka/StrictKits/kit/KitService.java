package org.nezxenka.StrictKits.kit;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.TimeFormat;

@RequiredArgsConstructor
public final class KitService {

    private final KitManager kits;
    private final PlayerDataManager players;
    private final Messages messages;

    public long remainingCooldown(PlayerData data, Kit kit) {
        long last = data.getCooldown(kit.getKey());
        if (last == 0L) {
            return 0L;
        }
        return Math.max(0L, kit.getCooldownMillis() - (System.currentTimeMillis() - last));
    }

    public boolean isReady(PlayerData data, Kit kit) {
        return kit.isOneTimeUse() ? !data.hasClaim(kit.getKey()) : remainingCooldown(data, kit) <= 0L;
    }

    public boolean give(Player player, Kit kit) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, messages.getDataNotLoaded());
            return false;
        }
        boolean admin = player.hasPermission("strictkits.admin");
        if (!admin && !canClaim(player, data, kit)) {
            return false;
        }
        if (kit.isEmpty()) {
            Messenger.send(player, messages.getKitEmpty());
            return false;
        }
        giveDirect(player, kit);
        if (!admin) {
            markClaimed(data, kit, System.currentTimeMillis());
        }
        return true;
    }

    public void giveDirect(Player player, Kit kit) {
        kit.applyTo(player);
        Messenger.send(player, messages.getKitReceived().format(kit.getName()));
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
            kit.applyTo(player);
            markClaimed(data, kit, now);
        }
    }

    private boolean canClaim(Player player, PlayerData data, Kit kit) {
        if (!kit.hasAccess(player)) {
            Messenger.send(player, messages.getNoPermission());
            return false;
        }
        if (kit.isOneTimeUse()) {
            if (data.hasClaim(kit.getKey())) {
                Messenger.send(player, messages.getKitAlreadyClaimed());
                return false;
            }
            return true;
        }
        long remaining = remainingCooldown(data, kit);
        if (remaining > 0L) {
            Messenger.send(player, messages.getCooldown().format(TimeFormat.format(remaining)));
            return false;
        }
        return true;
    }

    private static void markClaimed(PlayerData data, Kit kit, long now) {
        if (kit.isOneTimeUse()) {
            data.addClaim(kit.getKey(), now);
        } else {
            data.setCooldown(kit.getKey(), now);
        }
    }
}
