package org.nezxenka.strictkits.command.admin.edit;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.admin.KitSubCommand;
import org.nezxenka.strictkits.command.argument.CooldownArgument;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.section.AdminMessages;
import org.nezxenka.strictkits.message.sender.Messenger;

public final class SetCooldownCommand extends KitSubCommand {

    public SetCooldownCommand(ConfigManager configs, KitRegistry kits) {
        super(configs, kits, "setcooldown", 2);
    }

    @Override
    protected void execute(CommandSender sender, Kit kit, String[] args, AdminMessages messages) {
        CooldownArgument cooldown = CooldownArgument.parse(args[1], Kit.MAX_COOLDOWN_SECONDS);
        switch (cooldown.getStatus()) {
            case NOT_A_NUMBER -> Messenger.send(sender, messages.getCooldownNotANumber());
            case NEGATIVE -> Messenger.send(sender, messages.getCooldownNegative());
            case TOO_LARGE -> Messenger.send(sender, messages.getCooldownTooLarge().format(Kit.MAX_COOLDOWN_SECONDS));
            case VALID -> {
                kit.setCooldown(cooldown.getSeconds());
                kits.save(kit);
                Messenger.send(sender, messages.getCooldownUpdated().format(kit.getName(), cooldown.getSeconds()));
            }
        }
    }
}
