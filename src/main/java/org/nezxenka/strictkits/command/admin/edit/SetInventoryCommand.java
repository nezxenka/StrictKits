package org.nezxenka.strictkits.command.admin.edit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.model.KitContents;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;

public final class SetInventoryCommand extends KitSubCommand {

    public SetInventoryCommand(ConfigManager configs, KitRegistry kits) {
        super(configs, kits, "setinv", 1);
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, configs.getMessages().getPlayer().getPlayersOnly());
            return;
        }
        kit.setContents(KitContents.capture(player.getInventory()));
        kits.save(kit);
        Messenger.send(sender, messages.getInventoryUpdated().format(kit.getName()));
    }
}
