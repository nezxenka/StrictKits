package org.nezxenka.strictkits.command.admin.system;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.bootstrap.PluginReloader;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.message.sender.Messenger;

@RequiredArgsConstructor
public final class ReloadCommand implements SubCommand {

    private final ConfigManager configs;
    private final PluginReloader reloader;

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int kits = reloader.reload();
        Messenger.send(sender, configs.getMessages().getAdmin().getReloaded().format(kits));
    }
}
