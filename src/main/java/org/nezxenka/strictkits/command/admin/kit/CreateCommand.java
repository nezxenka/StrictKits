package org.nezxenka.strictkits.command.admin.kit;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.naming.KitNames;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;

@RequiredArgsConstructor
public final class CreateCommand implements SubCommand {

    private final ConfigManager configs;
    private final KitRegistry kits;

    @Override
    public String name() {
        return "create";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        AdminMessages messages = configs.getMessages().getAdmin();
        if (args.length != 1) {
            Messenger.send(sender, messages.getUsage(name()));
            return;
        }
        String name = args[0];
        if (!KitNames.isValid(name) || KitNames.isReserved(name)) {
            Messenger.send(sender, messages.getKitNameInvalid());
            return;
        }
        Kit kit = kits.create(name);
        if (kit == null) {
            Messenger.send(sender, messages.getKitExists());
            return;
        }
        Messenger.send(sender, messages.getKitCreated().format(kit.getName()));
    }
}
