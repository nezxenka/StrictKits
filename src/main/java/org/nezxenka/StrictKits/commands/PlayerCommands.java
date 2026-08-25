package org.nezxenka.StrictKits.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.config.Lang;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitManager;
import org.nezxenka.StrictKits.kit.KitService;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.Throttle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayerCommands implements TabExecutor {

    private final Main plugin;
    private final Throttle throttle;

    public PlayerCommands(Main plugin) {
        this.plugin = plugin;
        this.throttle = new Throttle(plugin.getSettings().getCommandThrottleMillis());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Lang lang = plugin.getLang();
        KitManager kits = plugin.getKits();
        KitService service = plugin.getKitService();

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                Messenger.send(sender, lang.getPlayersOnly());
                return true;
            }
            Player player = (Player) sender;
            if (!throttle.allow(player.getUniqueId())) {
                Messenger.send(player, lang.getThrottled());
                return true;
            }
            if (plugin.getSettings().isGuiDisplay()) {
                plugin.getKitMenu().open(player, 1);
            } else {
                sendOwnList(player);
            }
            return true;
        }

        String first = args[0];

        if (first.equalsIgnoreCase("help")) {
            for (String line : lang.getListHelp()) {
                Messenger.send(sender, line);
            }
            return true;
        }

        if (first.equalsIgnoreCase("list")) {
            if (plugin.getSettings().isListRequiresPermission() && !sender.hasPermission("strictkits.list")) {
                Messenger.send(sender, lang.getNoPermission());
                return true;
            }
            List<String> names = kits.names();
            if (names.isEmpty()) {
                Messenger.send(sender, lang.getNoKitServer());
                return true;
            }
            StringBuilder builder = new StringBuilder(lang.getKitListPrefix());
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    builder.append(lang.getKitListSeparator());
                }
                builder.append("§6").append(names.get(i));
            }
            Messenger.send(sender, builder.toString());
            return true;
        }

        if (first.equalsIgnoreCase("preview")) {
            if (!(sender instanceof Player)) {
                Messenger.send(sender, lang.getPlayersOnly());
                return true;
            }
            if (args.length < 2) {
                Messenger.send(sender, lang.getPreviewUsageError());
                return true;
            }
            Kit kit = kits.get(args[1]);
            if (kit == null) {
                Messenger.send(sender, lang.getKitDoesntExist());
                return true;
            }
            service.preview((Player) sender, kit);
            return true;
        }

        Kit kit = kits.get(first);
        if (kit == null) {
            Messenger.send(sender, lang.getKitDoesntExist());
            return true;
        }

        if (args.length >= 2) {
            if (!sender.hasPermission("strictkits.admin")) {
                Messenger.send(sender, lang.getNoPermission());
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Messenger.send(sender, lang.getPlayerOffline());
                return true;
            }
            service.giveDirect(target, kit);
            sender.sendMessage("§aНабор выдан игроку §e" + target.getName());
            return true;
        }

        if (!(sender instanceof Player)) {
            Messenger.send(sender, lang.getPlayersOnly());
            return true;
        }
        Player player = (Player) sender;
        if (!throttle.allow(player.getUniqueId())) {
            Messenger.send(player, lang.getThrottled());
            return true;
        }
        service.give(player, kit);
        return true;
    }

    private void sendOwnList(Player player) {
        Lang lang = plugin.getLang();
        KitService service = plugin.getKitService();
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, lang.getDataNotLoaded());
            return;
        }
        List<Kit> all = plugin.getKits().all();
        StringBuilder builder = new StringBuilder(lang.getKitListPrefix());
        int shown = 0;
        for (int i = 0; i < all.size(); i++) {
            Kit kit = all.get(i);
            if (!kit.hasAccess(player)) {
                continue;
            }
            if (shown > 0) {
                builder.append(lang.getKitListSeparator());
            }
            boolean ready = kit.isOneTimeUse()
                    ? !data.hasClaim(kit.getKey())
                    : service.remainingCooldown(data, kit) <= 0L;
            builder.append(ready ? "§a" : "§c").append(kit.getName());
            shown++;
        }
        if (shown == 0) {
            Messenger.send(player, lang.getNoAccess());
            return;
        }
        Messenger.send(player, builder.toString());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(plugin.getKits().size() + 3);
            options.add("help");
            options.add("list");
            options.add("preview");
            options.addAll(plugin.getKits().names());
            return filter(options, args[0]);
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
        String lower = prefix.toLowerCase();
        List<String> matches = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            String value = source.get(i);
            if (value.toLowerCase().startsWith(lower)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
