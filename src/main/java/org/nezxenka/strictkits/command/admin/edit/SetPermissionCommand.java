package org.nezxenka.strictkits.command.admin.edit;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.command.argument.PermissionArgument;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.permission.Permissions;
import org.nezxenka.strictkits.util.command.Completions;

import java.util.List;

public final class SetPermissionCommand extends KitSubCommand {

    public SetPermissionCommand(ConfigManager configs, KitRegistry kits) {
        super(configs, kits, "setperm", 2);
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        String permission = args[1].trim();
        if (!PermissionArgument.isValid(permission)) {
            Messenger.send(sender, messages.getPermissionInvalid());
            return;
        }
        kit.setPermission(permission);
        kits.save(kit);
        Messenger.send(sender, messages.getPermissionUpdated().format(kit.getName(), permission));
    }

    @Override
    protected List<String> completeArgument(CommandSender sender, String[] args) {
        Kit kit = kits.get(args[0]);
        String suggestion = kit == null ? Permissions.kit("kit") : kit.getPermission();
        return Completions.filter(List.of(suggestion), args[1]);
    }
}
