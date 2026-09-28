package org.nezxenka.strictkits.command.admin.system;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.message.sender.Messenger;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;
import org.nezxenka.strictkits.storage.api.StorageProvider;

@RequiredArgsConstructor
public final class StatsCommand implements SubCommand {

    private final ConfigManager configs;
    private final KitRegistry kits;
    private final PlayerDataManager players;
    private final StorageProvider storage;

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Messenger.send(sender, configs.getMessages().getAdmin().formatStats(
                storage.name(), kits.size(), players.getLoadedCount(), players.getWrites()));
    }
}
