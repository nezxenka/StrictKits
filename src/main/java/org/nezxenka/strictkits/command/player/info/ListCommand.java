package org.nezxenka.strictkits.command.player.info;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.permission.Permissions;

import java.util.List;

@RequiredArgsConstructor
public final class ListCommand implements SubCommand {

    private final ConfigManager configs;
    private final KitRegistry kits;

    @Override
    public String name() {
        return "list";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PlayerMessages messages = configs.getMessages().getPlayer();
        if (configs.getSettings().getAccess().isListRequiresPermission() && !sender.hasPermission(Permissions.LIST)) {
            Messenger.send(sender, messages.getNoPermission());
            return;
        }
        List<String> entries = kits.names().stream()
                .map(name -> messages.getListEntry().format(name))
                .toList();
        if (entries.isEmpty()) {
            Messenger.send(sender, messages.getNoKitsOnServer());
            return;
        }
        Messenger.send(sender, messages.formatList(entries));
    }
}
