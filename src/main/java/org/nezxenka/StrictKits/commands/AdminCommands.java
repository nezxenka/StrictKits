package org.nezxenka.StrictKits.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.util.Messenger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AdminCommands implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            "create", "remove", "setinv", "setcooldown", "setonetimeuse",
            "setfirstjoinkit", "seticon", "setperm", "purge", "reload", "stats", "version"));

    private static final List<String> BOOLEANS = Collections.unmodifiableList(Arrays.asList("true", "false"));

    private static final List<String> RESERVED = Collections.unmodifiableList(Arrays.asList(
            "list", "preview", "help"));

    private final Main plugin;

    public AdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length == 0) {
            Messenger.send(sender, messages.getAdminHelp());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return create(sender, messages, args);
            case "remove":
                return remove(sender, messages, args);
            case "setinv":
                return setInventory(sender, messages, args);
            case "setcooldown":
                return setCooldown(sender, messages, args);
            case "setonetimeuse":
                return setFlag(sender, messages, args, true);
            case "setfirstjoinkit":
                return setFlag(sender, messages, args, false);
            case "seticon":
                return setIcon(sender, messages, args);
            case "setperm":
                return setPermission(sender, messages, args);
            case "purge":
                return purge(sender, messages, args);
            case "reload":
                plugin.reloadPlugin();
                Messenger.send(sender, plugin.getMessages().getAdminReloaded(plugin.getKits().size()));
                return true;
            case "stats":
                Messenger.send(sender, messages.getAdminStats(plugin.getStorageName(), plugin.getCacheName(),
                        plugin.getKits().size(), plugin.getPlayers()));
                return true;
            case "version":
                Messenger.send(sender, messages.getAdminVersion());
                return true;
            default:
                Messenger.send(sender, messages.getAdminHelp());
                return true;
        }
    }

    private Kit require(CommandSender sender, Messages messages, String name) {
        Kit kit = plugin.getKits().get(name);
        if (kit == null) {
            Messenger.send(sender, messages.getAdminKitNotFound());
        }
        return kit;
    }

    private boolean usage(CommandSender sender, Messages messages, String subcommand) {
        Messenger.send(sender, messages.getUsage(subcommand));
        return true;
    }

    private boolean create(CommandSender sender, Messages messages, String[] args) {
        if (args.length != 2) {
            return usage(sender, messages, "create");
        }
        String name = args[1];
        if (!Kit.isValidName(name) || RESERVED.contains(name.toLowerCase())) {
            Messenger.send(sender, messages.getAdminKitNameInvalid());
            return true;
        }
        Kit kit = plugin.getKits().create(name);
        if (kit == null) {
            Messenger.send(sender, messages.getAdminKitExists());
            return true;
        }
        Messenger.send(sender, messages.getAdminKitCreated(kit.getName()));
        return true;
    }

    private boolean remove(CommandSender sender, Messages messages, String[] args) {
        if (args.length != 2) {
            return usage(sender, messages, "remove");
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        plugin.getKits().remove(kit);
        plugin.getPlayers().onKitRemoved(kit.getKey());
        Messenger.send(sender, messages.getAdminKitRemoved(kit.getName()));
        return true;
    }

    private boolean setInventory(CommandSender sender, Messages messages, String[] args) {
        if (!(sender instanceof Player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return true;
        }
        if (args.length != 2) {
            return usage(sender, messages, "setinv");
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        Player player = (Player) sender;
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] main = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (i >= 36 && i <= 39) {
                continue;
            }
            main[i] = contents[i];
        }
        kit.setMainContent(main);
        kit.setArmorContent(player.getInventory().getArmorContents());
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminInventoryUpdated(kit.getName()));
        return true;
    }

    private boolean setCooldown(CommandSender sender, Messages messages, String[] args) {
        if (args.length != 3) {
            return usage(sender, messages, "setcooldown");
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        long seconds;
        try {
            seconds = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            Messenger.send(sender, messages.getAdminCooldownNotANumber());
            return true;
        }
        if (seconds < 0L) {
            Messenger.send(sender, messages.getAdminCooldownNegative());
            return true;
        }
        if (seconds > Kit.MAX_COOLDOWN_SECONDS) {
            Messenger.send(sender, messages.getAdminCooldownTooLarge(Kit.MAX_COOLDOWN_SECONDS));
            return true;
        }
        kit.setCooldown(seconds);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminCooldownUpdated(kit.getName(), seconds));
        return true;
    }

    private boolean setFlag(CommandSender sender, Messages messages, String[] args, boolean oneTimeUse) {
        String subcommand = oneTimeUse ? "setonetimeuse" : "setfirstjoinkit";
        if (args.length != 3) {
            return usage(sender, messages, subcommand);
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false")) {
            return usage(sender, messages, subcommand);
        }
        boolean value = Boolean.parseBoolean(args[2]);
        if (oneTimeUse) {
            kit.setOneTimeUse(value);
        } else {
            kit.setFirstTimeJoinKit(value);
        }
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminFlagUpdated(
                oneTimeUse ? "OneTimeUse" : "FirstJoinKit", kit.getName(), value));
        return true;
    }

    private boolean setIcon(CommandSender sender, Messages messages, String[] args) {
        if (!(sender instanceof Player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return true;
        }
        if (args.length != 2) {
            return usage(sender, messages, "seticon");
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        Player player = (Player) sender;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Messenger.send(sender, messages.getAdminIconHandEmpty());
            return true;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            Messenger.send(sender, messages.getAdminIconWithoutName());
            return true;
        }
        for (Kit other : plugin.getKits().all()) {
            if (other != kit && hand.isSimilar(other.getIcon())) {
                Messenger.send(sender, messages.getAdminIconAlreadyUsed(other.getName()));
                return true;
            }
        }
        kit.setIcon(hand);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminIconUpdated(kit.getName()));
        return true;
    }

    private boolean setPermission(CommandSender sender, Messages messages, String[] args) {
        if (args.length != 3) {
            return usage(sender, messages, "setperm");
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        String perm = args[2].trim();
        if (!perm.matches("^[A-Za-z0-9._-]{1,64}$")) {
            Messenger.send(sender, messages.getAdminKitNameInvalid());
            return true;
        }
        kit.setPermission(perm);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminPermissionUpdated(kit.getName(), perm));
        return true;
    }

    private boolean purge(CommandSender sender, Messages messages, String[] args) {
        if (args.length != 2) {
            return usage(sender, messages, "purge");
        }
        Kit kit = require(sender, messages, args[1]);
        if (kit == null) {
            return true;
        }
        if (kit.isOneTimeUse() || kit.getCooldown() <= 0L) {
            Messenger.send(sender, messages.getAdminPurgeNothing());
            return true;
        }
        plugin.getPlayers().purgeExpired(kit.getKey(), kit.getCooldownMillis());
        Messenger.send(sender, messages.getAdminPurgeStarted(kit.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return PlayerCommands.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create") || sub.equals("reload") || sub.equals("stats") || sub.equals("version")) {
                return Collections.emptyList();
            }
            return PlayerCommands.filter(plugin.getKits().names(), args[1]);
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setonetimeuse") || sub.equals("setfirstjoinkit")) {
                return PlayerCommands.filter(BOOLEANS, args[2]);
            }
            if (sub.equals("setperm")) {
                Kit kit = plugin.getKits().get(args[1]);
                return PlayerCommands.filter(
                        Collections.singletonList(kit == null ? "strictkits.kits.kit" : kit.getPermission()), args[2]);
            }
        }
        return Collections.emptyList();
    }
}
