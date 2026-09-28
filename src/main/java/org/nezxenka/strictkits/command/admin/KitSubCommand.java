package org.nezxenka.strictkits.command.admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.util.command.Completions;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class KitSubCommand implements SubCommand {

    protected final ConfigManager configs;
    protected final KitRegistry kits;
    private final String name;
    private final int argumentCount;

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final void execute(CommandSender sender, String[] args) {
        AdminMessages messages = configs.getMessages().getAdmin();
        if (args.length != argumentCount) {
            sendUsage(sender, messages);
            return;
        }
        Kit kit = kits.get(args[0]);
        if (kit == null) {
            Messenger.send(sender, messages.getKitNotFound());
            return;
        }
        execute(sender, kit, args, messages);
    }

    @Override
    public final List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Completions.filter(kits.names(), args[0]);
        }
        return args.length == argumentCount ? completeArgument(sender, args) : Collections.emptyList();
    }

    protected abstract void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages);

    protected List<String> completeArgument(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    protected final void sendUsage(CommandSender sender, AdminMessages messages) {
        Messenger.send(sender, messages.getUsage(name));
    }
}
