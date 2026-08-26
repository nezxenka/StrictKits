package org.nezxenka.StrictKits.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitManager;
import org.nezxenka.StrictKits.kit.KitService;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.Throttle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PlayerCommands implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(
            Arrays.asList("help", "list", "preview"));

    private final Main plugin;
    private final Throttle throttle;

    public PlayerCommands(Main plugin) {
        this.plugin = plugin;
        this.throttle = new Throttle();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        KitManager kits = plugin.getKits();
        KitService service = plugin.getKitService();

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                Messenger.send(sender, messages.getPlayersOnly());
                return true;
            }
            Player player = (Player) sender;
            if (!throttle.allow(player.getUniqueId(), plugin.getSettings().getCommandThrottleMillis())) {
                Messenger.send(player, messages.getThrottled());
                return true;
            }
            if (plugin.getSettings().isGuiDisplay()) {
                plugin.getKitMenu().open(player, 1);
            } else {
                sendOwnList(player, messages);
            }
            return true;
        }

        String first = args[0];

        if (first.equalsIgnoreCase("help")) {
            Messenger.send(sender, messages.getPlayerHelp());
            return true;
        }

        if (first.equalsIgnoreCase("list")) {
            if (plugin.getSettings().isListRequiresPermission() && !sender.hasPermission("strictkits.list")) {
                Messenger.send(sender, messages.getNoPermission());
                return true;
            }
            List<String> names = kits.names();
            if (names.isEmpty()) {
                Messenger.send(sender, messages.getNoKitsOnServer());
                return true;
            }
            StringBuilder builder = new StringBuilder(names.size() * 16);
            builder.append(messages.getListPrefix());
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    builder.append(messages.getListSeparator());
                }
                builder.append(messages.getListEntry(names.get(i)));
            }
            Messenger.send(sender, builder.toString());
            return true;
        }

        if (first.equalsIgnoreCase("preview")) {
            if (!(sender instanceof Player)) {
                Messenger.send(sender, messages.getPlayersOnly());
                return true;
            }
            if (args.length < 2) {
                Messenger.send(sender, messages.getPreviewUsage());
                return true;
            }
            Kit kit = kits.get(args[1]);
            if (kit == null) {
                Messenger.send(sender, messages.getKitNotFound());
                return true;
            }
            service.preview((Player) sender, kit);
            return true;
        }

        Kit kit = kits.get(first);
        if (kit == null) {
            Messenger.send(sender, messages.getKitNotFound());
            return true;
        }

        if (args.length >= 2) {
            if (!sender.hasPermission("strictkits.admin")) {
                Messenger.send(sender, messages.getNoPermission());
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Messenger.send(sender, messages.getPlayerOffline());
                return true;
            }
            service.giveDirect(target, kit);
            Messenger.send(sender, messages.getAdminKitGiven(kit.getName(), target.getName()));
            return true;
        }

        if (!(sender instanceof Player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return true;
        }
        Player player = (Player) sender;
        if (!throttle.allow(player.getUniqueId(), plugin.getSettings().getCommandThrottleMillis())) {
            Messenger.send(player, messages.getThrottled());
            return true;
        }
        service.give(player, kit);
        return true;
    }

    private void sendOwnList(Player player, Messages messages) {
        KitService service = plugin.getKitService();
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, messages.getDataNotLoaded());
            return;
        }
        List<Kit> all = plugin.getKits().all();
        StringBuilder builder = new StringBuilder(all.size() * 16);
        builder.append(messages.getListPrefix());
        int shown = 0;
        for (Kit kit : all) {
            if (!kit.hasAccess(player)) {
                continue;
            }
            if (shown > 0) {
                builder.append(messages.getListSeparator());
            }
            boolean ready = kit.isOneTimeUse()
                    ? !data.hasClaim(kit.getKey())
                    : service.remainingCooldown(data, kit) <= 0L;
            builder.append(messages.getListEntry(kit.getName(), ready));
            shown++;
        }
        if (shown == 0) {
            Messenger.send(player, messages.getNoAccess());
            return;
        }
        Messenger.send(player, builder.toString());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = filter(SUBCOMMANDS, args[0]);
            collect(matches, plugin.getKits().names(), args[0]);
            return matches;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return filter(plugin.getKits().names(), args[1]);
        }
        if (args.length == 2 && sender.hasPermission("strictkits.admin")) {
            return null;
        }
        return Collections.emptyList();
    }

    static List<String> filter(List<String> source, String prefix) {
        List<String> matches = new ArrayList<>(Math.min(source.size(), 16));
        collect(matches, source, prefix);
        return matches;
    }

    static void collect(List<String> target, List<String> source, String prefix) {
        int length = prefix.length();
        for (String value : source) {
            if (value.length() >= length && value.regionMatches(true, 0, prefix, 0, length)) {
                target.add(value);
            }
        }
    }
}
