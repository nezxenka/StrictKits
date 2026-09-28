package org.nezxenka.strictkits.command.player.view;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.menu.view.KitPreviewMenu;
import org.nezxenka.strictkits.message.section.PlayerMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.util.command.Completions;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public final class PreviewCommand implements SubCommand {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final KitPreviewMenu previewMenu;

    @Override
    public String name() {
        return "preview";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PlayerMessages messages = configs.getMessages().getPlayer();
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        if (args.length < 1) {
            Messenger.send(sender, messages.getPreviewUsage());
            return;
        }
        Kit kit = kits.get(args[0]);
        if (kit == null) {
            Messenger.send(sender, messages.getKitNotFound());
            return;
        }
        previewMenu.open(player, kit);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length == 1 ? Completions.filter(kits.names(), args[0]) : Collections.emptyList();
    }
}
