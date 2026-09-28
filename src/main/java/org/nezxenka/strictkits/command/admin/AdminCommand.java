package org.nezxenka.strictkits.command.admin;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.bootstrap.PluginReloader;
import org.nezxenka.strictkits.command.admin.data.PurgeCommand;
import org.nezxenka.strictkits.command.admin.edit.SetCooldownCommand;
import org.nezxenka.strictkits.command.admin.edit.SetFlagCommand;
import org.nezxenka.strictkits.command.admin.edit.SetIconCommand;
import org.nezxenka.strictkits.command.admin.edit.SetInventoryCommand;
import org.nezxenka.strictkits.command.admin.edit.SetPermissionCommand;
import org.nezxenka.strictkits.command.admin.kit.CreateCommand;
import org.nezxenka.strictkits.command.admin.kit.RemoveCommand;
import org.nezxenka.strictkits.command.admin.system.ReloadCommand;
import org.nezxenka.strictkits.command.admin.system.StatsCommand;
import org.nezxenka.strictkits.command.admin.system.VersionCommand;
import org.nezxenka.strictkits.command.api.SubCommandRouter;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;
import org.nezxenka.strictkits.storage.api.StorageProvider;

public final class AdminCommand extends SubCommandRouter {

    private final ConfigManager configs;

    public AdminCommand(ConfigManager configs, KitRegistry kits, PlayerDataManager players,
                        StorageProvider storage, PluginReloader reloader) {
        this.configs = configs;
        register(new CreateCommand(configs, kits));
        register(new RemoveCommand(configs, kits, players));
        register(new SetInventoryCommand(configs, kits));
        register(new SetCooldownCommand(configs, kits));
        register(new SetFlagCommand(configs, kits, "setonetimeuse", "OneTimeUse", Kit::setOneTimeUse));
        register(new SetFlagCommand(configs, kits, "setfirstjoinkit", "FirstJoinKit", Kit::setFirstTimeJoinKit));
        register(new SetIconCommand(configs, kits));
        register(new SetPermissionCommand(configs, kits));
        register(new PurgeCommand(configs, kits, players));
        register(new ReloadCommand(configs, reloader));
        register(new StatsCommand(configs, kits, players, storage));
        register(new VersionCommand(configs));
    }

    @Override
    protected void onEmpty(CommandSender sender) {
        Messenger.send(sender, configs.getMessages().getAdmin().getHelp());
    }

    @Override
    protected void onUnknown(CommandSender sender, String[] args) {
        Messenger.send(sender, configs.getMessages().getAdmin().getHelp());
    }
}
