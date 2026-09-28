package org.nezxenka.strictkits.command.admin.data;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;

public final class PurgeCommand extends KitSubCommand {

    private final PlayerDataManager players;

    public PurgeCommand(ConfigManager configs, KitRegistry kits, PlayerDataManager players) {
        super(configs, kits, "purge", 1);
        this.players = players;
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        if (kit.isOneTimeUse() || kit.getCooldown() <= 0L) {
            Messenger.send(sender, messages.getPurgeNothing());
            return;
        }
        players.purgeExpired(kit.getKey(), kit.getCooldownMillis());
        Messenger.send(sender, messages.getPurgeStarted().format(kit.getName()));
    }
}
