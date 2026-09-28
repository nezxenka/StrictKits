package org.nezxenka.strictkits.command.player.info;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.message.sender.Messenger;

@RequiredArgsConstructor
public final class HelpCommand implements SubCommand {

    private final ConfigManager configs;

    @Override
    public String name() {
        return "help";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Messenger.send(sender, configs.getMessages().getPlayer().getHelp());
    }
}
