package org.nezxenka.strictkits.command.admin.system;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.message.sender.Messenger;

@RequiredArgsConstructor
public final class VersionCommand implements SubCommand {

    private final ConfigManager configs;

    @Override
    public String name() {
        return "version";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Messenger.send(sender, configs.getMessages().getAdmin().getVersion());
    }
}
