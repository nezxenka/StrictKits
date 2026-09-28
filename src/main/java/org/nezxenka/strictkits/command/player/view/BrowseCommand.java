package org.nezxenka.strictkits.command.player.view;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.command.player.CommandThrottle;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.claim.ClaimRules;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.menu.view.KitListMenu;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.player.data.PlayerData;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;

import java.util.List;

@RequiredArgsConstructor
public final class BrowseCommand {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final PlayerDataManager players;
    private final KitListMenu listMenu;
    private final CommandThrottle throttle;

    public void execute(CommandSender sender) {
        PlayerMessages messages = configs.getMessages().getPlayer();
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        if (!throttle.tryPass(player)) {
            return;
        }
        if (configs.getSettings().getMenu().isEnabled()) {
            listMenu.open(player, 1);
        } else {
            sendAvailableKits(player, messages);
        }
    }

    private void sendAvailableKits(Player player, PlayerMessages messages) {
        PlayerData data = players.getOrRequest(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, messages.getDataNotLoaded());
            return;
        }
        List<String> entries = kits.all().stream()
                .filter(kit -> kit.hasAccess(player))
                .map(kit -> (ClaimRules.isAvailable(data, kit) ? messages.getListEntryReady() : messages.getListEntryCooldown())
                        .format(kit.getName()))
                .toList();
        if (entries.isEmpty()) {
            Messenger.send(player, messages.getNoAccess());
            return;
        }
        Messenger.send(player, messages.formatList(entries));
    }
}
