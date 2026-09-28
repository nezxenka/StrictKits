package org.nezxenka.strictkits.command.player;

import org.bukkit.command.CommandSender;
import org.nezxenka.strictkits.command.api.SubCommandRouter;
import org.nezxenka.strictkits.command.player.claim.ClaimCommand;
import org.nezxenka.strictkits.command.player.info.HelpCommand;
import org.nezxenka.strictkits.command.player.info.ListCommand;
import org.nezxenka.strictkits.command.player.view.BrowseCommand;
import org.nezxenka.strictkits.command.player.view.PreviewCommand;
import org.nezxenka.strictkits.config.ConfigManager;
import org.nezxenka.strictkits.kit.claim.ClaimFeedback;
import org.nezxenka.strictkits.kit.claim.ClaimService;
import org.nezxenka.strictkits.kit.registry.KitRegistry;
import org.nezxenka.strictkits.menu.view.KitListMenu;
import org.nezxenka.strictkits.menu.view.KitPreviewMenu;
import org.nezxenka.strictkits.permission.Permissions;
import org.nezxenka.strictkits.player.manager.PlayerDataManager;
import org.nezxenka.strictkits.util.command.Completions;

import java.util.Collections;
import java.util.List;

public final class KitCommand extends SubCommandRouter {

    private final KitRegistry kits;
    private final BrowseCommand browse;
    private final ClaimCommand claim;

    public KitCommand(ConfigManager configs, KitRegistry kits, PlayerDataManager players, ClaimService claims,
                      ClaimFeedback feedback, KitListMenu listMenu, KitPreviewMenu previewMenu) {
        this.kits = kits;
        CommandThrottle throttle = new CommandThrottle(configs);
        this.browse = new BrowseCommand(configs, kits, players, listMenu, throttle);
        this.claim = new ClaimCommand(configs, kits, claims, feedback, throttle);
        register(new HelpCommand(configs));
        register(new ListCommand(configs, kits));
        register(new PreviewCommand(configs, kits, previewMenu));
    }

    @Override
    protected void onEmpty(CommandSender sender) {
        browse.execute(sender);
    }

    @Override
    protected void onUnknown(CommandSender sender, String[] args) {
        claim.execute(sender, args);
    }

    @Override
    protected List<String> completeFirst(CommandSender sender, String prefix) {
        List<String> matches = super.completeFirst(sender, prefix);
        Completions.collect(matches, kits.names(), prefix);
        return matches;
    }

    @Override
    protected List<String> completeUnknown(CommandSender sender, String[] args) {
        return args.length == 2 && sender.hasPermission(Permissions.ADMIN) ? null : Collections.emptyList();
    }
}
