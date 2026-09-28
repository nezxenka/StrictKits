package org.nezxenka.strictkits.kit.claim;

import lombok.experimental.UtilityClass;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.player.data.PlayerData;

@UtilityClass
public class ClaimRules {

    public static long remainingCooldown(PlayerData data, Kit kit) {
        long last = data.getCooldown(kit.getKey());
        if (last == 0L) {
            return 0L;
        }
        return Math.max(0L, kit.getCooldownMillis() - (System.currentTimeMillis() - last));
    }

    public static boolean isAvailable(PlayerData data, Kit kit) {
        return kit.isOneTimeUse() ? !data.hasClaim(kit.getKey()) : remainingCooldown(data, kit) <= 0L;
    }
}
