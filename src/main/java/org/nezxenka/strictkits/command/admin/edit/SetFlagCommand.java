package org.nezxenka.strictkits.command.admin.edit;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.command.argument.BooleanArgument;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class SetFlagCommand extends KitSubCommand {

    private final String flag;
    private final BiConsumer<Kit, Boolean> setter;

    public SetFlagCommand(ConfigManager configs, KitRegistry kits, String name, String flag,
                          BiConsumer<Kit, Boolean> setter) {
        super(configs, kits, name, 2);
        this.flag = flag;
        this.setter = setter;
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        Optional<Boolean> value = BooleanArgument.parse(args[1]);
        if (value.isEmpty()) {
            sendUsage(sender, messages);
            return;
        }
        setter.accept(kit, value.get());
        kits.save(kit);
        Messenger.send(sender, messages.getFlagUpdated().format(flag, kit.getName(), value.get()));
    }

    @Override
    protected List<String> completeArgument(CommandSender sender, String[] args) {
        return BooleanArgument.complete(args[1]);
    }
}
