package org.nezxenka.StrictKits.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitService;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.util.Completions;
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.Throttle;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public final class PlayerCommands implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("help", "list", "preview");

    private final Main plugin;
    private final Throttle throttle = new Throttle();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length == 0) {
            openKits(sender, messages);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help" -> Messenger.send(sender, messages.getPlayerHelp());
            case "list" -> sendKitList(sender, messages);
            case "preview" -> preview(sender, messages, args);
            default -> claim(sender, messages, args);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = Completions.filter(SUBCOMMANDS, args[0]);
            Completions.collect(matches, plugin.getKits().names(), args[0]);
            return matches;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return Completions.filter(plugin.getKits().names(), args[1]);
        }
        if (args.length == 2 && sender.hasPermission("strictkits.admin")) {
            return null;
        }
        return Collections.emptyList();
    }

    private void openKits(CommandSender sender, Messages messages) {
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        if (isThrottled(player, messages)) {
            return;
        }
        if (plugin.getSettings().isGuiDisplay()) {
            plugin.getKitMenu().open(player, 1);
        } else {
            sendAvailableKits(player, messages);
        }
    }

    private void sendKitList(CommandSender sender, Messages messages) {
        if (plugin.getSettings().isListRequiresPermission() && !sender.hasPermission("strictkits.list")) {
            Messenger.send(sender, messages.getNoPermission());
            return;
        }
        List<String> entries = plugin.getKits().names().stream()
                .map(name -> messages.getListEntry().format(name))
                .toList();
        if (entries.isEmpty()) {
            Messenger.send(sender, messages.getNoKitsOnServer());
            return;
        }
        Messenger.send(sender, joinList(messages, entries));
    }

    private void sendAvailableKits(Player player, Messages messages) {
        PlayerData data = plugin.getPlayers().get(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, messages.getDataNotLoaded());
            return;
        }
        KitService service = plugin.getKitService();
        List<String> entries = plugin.getKits().all().stream()
                .filter(kit -> kit.hasAccess(player))
                .map(kit -> (service.isReady(data, kit) ? messages.getListEntryReady() : messages.getListEntryCooldown())
                        .format(kit.getName()))
                .toList();
        if (entries.isEmpty()) {
            Messenger.send(player, messages.getNoAccess());
            return;
        }
        Messenger.send(player, joinList(messages, entries));
    }

    private void preview(CommandSender sender, Messages messages, String[] args) {
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        if (args.length < 2) {
            Messenger.send(sender, messages.getPreviewUsage());
            return;
        }
        Kit kit = plugin.getKits().get(args[1]);
        if (kit == null) {
            Messenger.send(sender, messages.getKitNotFound());
            return;
        }
        plugin.getKitMenu().preview(player, kit);
    }

    private void claim(CommandSender sender, Messages messages, String[] args) {
        Kit kit = plugin.getKits().get(args[0]);
        if (kit == null) {
            Messenger.send(sender, messages.getKitNotFound());
            return;
        }
        if (args.length >= 2) {
            giveToPlayer(sender, messages, kit, args[1]);
            return;
        }
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        if (!isThrottled(player, messages)) {
            plugin.getKitService().give(player, kit);
        }
    }

    private void giveToPlayer(CommandSender sender, Messages messages, Kit kit, String targetName) {
        if (!sender.hasPermission("strictkits.admin")) {
            Messenger.send(sender, messages.getNoPermission());
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Messenger.send(sender, messages.getPlayerOffline());
            return;
        }
        if (kit.isEmpty()) {
            Messenger.send(sender, messages.getKitEmpty());
            return;
        }
        plugin.getKitService().giveDirect(target, kit);
        Messenger.send(sender, messages.getAdminKitGiven().format(kit.getName(), target.getName()));
    }

    private boolean isThrottled(Player player, Messages messages) {
        if (throttle.allow(player.getUniqueId(), plugin.getSettings().getCommandThrottleMillis())) {
            return false;
        }
        Messenger.send(player, messages.getThrottled());
        return true;
    }

    private static String joinList(Messages messages, List<String> entries) {
        return messages.getListPrefix() + String.join(messages.getListSeparator(), entries);
    }
}
