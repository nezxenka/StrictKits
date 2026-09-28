package org.nezxenka.strictkits.command.admin.edit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.util.item.ItemStacks;

public final class SetIconCommand extends KitSubCommand {

    public SetIconCommand(ConfigManager configs, KitRegistry kits) {
        super(configs, kits, "seticon", 1);
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, configs.getMessages().getPlayer().getPlayersOnly());
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (ItemStacks.isAir(hand)) {
            Messenger.send(sender, messages.getIconHandEmpty());
            return;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            Messenger.send(sender, messages.getIconWithoutName());
            return;
        }
        kit.setIcon(hand);
        kits.save(kit);
        Messenger.send(sender, messages.getIconUpdated().format(kit.getName()));
    }
}
