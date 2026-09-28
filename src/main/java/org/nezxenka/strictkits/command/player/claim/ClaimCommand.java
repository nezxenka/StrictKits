package org.nezxenka.strictkits.command.player.claim;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.command.player.CommandThrottle;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.claim.ClaimFeedback;
import org.nezxenka.strictkits.kit.claim.ClaimService;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.Messages;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.permission.Permissions;

@RequiredArgsConstructor
public final class ClaimCommand {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final ClaimService claims;
    private final ClaimFeedback feedback;
    private final CommandThrottle throttle;

    public void execute(CommandSender sender, String[] args) {
        PlayerMessages messages = configs.getMessages().getPlayer();
        Kit kit = kits.get(args[0]);
        if (kit == null) {
            Messenger.send(sender, messages.getKitNotFound());
            return;
        }
        if (args.length >= 2) {
            giveToPlayer(sender, configs.getMessages(), kit, args[1]);
            return;
        }
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        if (throttle.tryPass(player)) {
            feedback.send(player, kit, claims.claim(player, kit));
        }
    }

    private void giveToPlayer(CommandSender sender, Messages messages, Kit kit, String targetName) {
        PlayerMessages text = messages.getPlayer();
        if (!sender.hasPermission(Permissions.ADMIN)) {
            Messenger.send(sender, text.getNoPermission());
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Messenger.send(sender, text.getPlayerOffline());
            return;
        }
        if (kit.isEmpty()) {
            Messenger.send(sender, text.getKitEmpty());
            return;
        }
        claims.grant(target, kit);
        feedback.sendReceived(target, kit);
        Messenger.send(sender, messages.getAdmin().getKitGiven().format(kit.getName(), target.getName()));
    }
}
