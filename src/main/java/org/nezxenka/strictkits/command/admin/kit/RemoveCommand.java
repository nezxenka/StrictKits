package org.nezxenka.strictkits.command.admin.kit;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;

public final class RemoveCommand extends KitSubCommand {

    private final PlayerDataManager players;

    public RemoveCommand(ConfigManager configs, KitRegistry kits, PlayerDataManager players) {
        super(configs, kits, "remove", 1);
        this.players = players;
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        kits.remove(kit);
        players.onKitRemoved(kit.getKey());
        Messenger.send(sender, messages.getKitRemoved().format(kit.getName()));
    }
}
