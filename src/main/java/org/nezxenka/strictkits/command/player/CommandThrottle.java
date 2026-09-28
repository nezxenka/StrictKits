package org.nezxenka.strictkits.command.player;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.util.rate.Throttle;

@RequiredArgsConstructor
public final class CommandThrottle {

    private final ConfigManager configs;
    private final Throttle throttle = new Throttle();

    public boolean tryPass(Player player) {
        if (throttle.allow(player.getUniqueId(), configs.getSettings().getCommand().getThrottleMillis())) {
            return true;
        }
        Messenger.send(player, configs.getMessages().getPlayer().getThrottled());
        return false;
    }
}
