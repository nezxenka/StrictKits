package org.nezxenka.strictkits.kit.claim;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.message.Messages;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;

@RequiredArgsConstructor
public final class ClaimFeedback {

    private final ConfigManager configs;

    public void send(Player player, Kit kit, ClaimResult result) {
        Messages messages = configs.getMessages();
        PlayerMessages text = messages.getPlayer();
        String message = switch (result.getStatus()) {
            case GRANTED -> text.getKitReceived().format(kit.getName());
            case DATA_NOT_LOADED -> text.getDataNotLoaded();
            case NO_PERMISSION -> text.getNoPermission();
            case ALREADY_CLAIMED -> text.getKitAlreadyClaimed();
            case ON_COOLDOWN -> messages.formatCooldown(result.getRemainingMillis());
            case EMPTY -> text.getKitEmpty();
        };
        Messenger.send(player, message);
    }

    public void sendReceived(Player player, Kit kit) {
        send(player, kit, ClaimResult.granted());
    }
}
