package org.nezxenka.StrictKits.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.kit.KitManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AdminCommands implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            "create", "remove", "setinv", "setcooldown", "setonetimeuse",
            "setfirstjoinkit", "seticon", "setperm", "purge", "reload", "stats", "version"));

    private static final List<String> BOOLEANS = Collections.unmodifiableList(Arrays.asList("true", "false"));

    private final Main plugin;

    public AdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        KitManager kits = plugin.getKits();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                return create(sender, args);
            case "remove":
                return remove(sender, args);
            case "setinv":
                return setInventory(sender, args);
            case "setcooldown":
                return setCooldown(sender, args);
            case "setonetimeuse":
                return setFlag(sender, args, true);
            case "setfirstjoinkit":
                return setFlag(sender, args, false);
            case "seticon":
                return setIcon(sender, args);
            case "setperm":
                return setPermission(sender, args);
            case "purge":
                return purge(sender, args);
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage("§7[SK] §aКонфигурация перезагружена, китов: §e" + kits.size());
                return true;
            case "stats":
                sender.sendMessage("§7[SK] §aХранилище: §e" + plugin.getStorageName()
                        + " §a| Кэш: §e" + plugin.getCacheName());
                sender.sendMessage("§7[SK] §a" + plugin.getPlayers().stats());
                sender.sendMessage("§7[SK] §aКитов: §e" + kits.size());
                return true;
            case "version":
                sender.sendMessage("§7[SK] §aВерсия: §e" + plugin.getDescription().getVersion());
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§cStrictKits §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§8► §c/sk create §6<кит>");
        sender.sendMessage("§8► §c/sk remove §6<кит>");
        sender.sendMessage("§8► §c/sk setinv §6<кит>");
        sender.sendMessage("§8► §c/sk setcooldown §6<кит> <секунды>");
        sender.sendMessage("§8► §c/sk setonetimeuse §6<кит> <true:false>");
        sender.sendMessage("§8► §c/sk setfirstjoinkit §6<кит> <true:false>");
        sender.sendMessage("§8► §c/sk seticon §6<кит>");
        sender.sendMessage("§8► §c/sk setperm §6<кит> <право>");
        sender.sendMessage("§8► §c/sk purge §6<кит>");
        sender.sendMessage("§8► §c/sk reload");
        sender.sendMessage("§8► §c/sk stats");
        sender.sendMessage("§8► §c/sk version");
    }

    private Kit require(CommandSender sender, String name) {
        Kit kit = plugin.getKits().get(name);
        if (kit == null) {
            sender.sendMessage("§7[SK] §cЭтого набора не существует");
        }
        return kit;
    }

    private boolean usage(CommandSender sender, String text) {
        sender.sendMessage("§7[SK] §cИспользование: " + text);
        return true;
    }

    private boolean create(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return usage(sender, "/sk create <кит>");
        }
        String name = args[1];
        if (name.length() > 32 || name.equalsIgnoreCase("list")
                || name.equalsIgnoreCase("preview") || name.equalsIgnoreCase("help")) {
            sender.sendMessage("§7[SK] §cНедопустимое имя набора");
            return true;
        }
        Kit kit = plugin.getKits().create(name);
        if (kit == null) {
            sender.sendMessage("§7[SK] §cЭтот набор уже существует");
            return true;
        }
        sender.sendMessage("§7[SK] §aВы успешно создали набор §e" + name);
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return usage(sender, "/sk remove <кит>");
        }
        Kit kit = require(sender, args[1]);
        if (kit == null) {
            return true;
        }
        plugin.getKits().remove(kit);
        plugin.getPlayers().onKitRemoved(kit.getKey());
        sender.sendMessage("§7[SK] §aВы успешно §cудалили §aнабор §e" + kit.getName());
        return true;
    }

    private boolean setInventory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLang().getPlayersOnly());
            return true;
        }
        if (args.length != 2) {
            return usage(sender, "/sk setinv <кит>");
        }
        Kit kit = require(sender, args[1]);
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
        sender.sendMessage("§7[SK] §aВы успешно установили инвентарь набора §e" + kit.getName());
        return true;
    }

    private boolean setCooldown(CommandSender sender, String[] args) {
        if (args.length != 3) {
            return usage(sender, "/sk setcooldown <кит> <секунды>");
        }
        Kit kit = require(sender, args[1]);
        if (kit == null) {
            return true;
        }
        long seconds;
        try {
            seconds = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§7[SK] §cЗадержка должна быть числом");
            return true;
        }
        if (seconds < 0L) {
            sender.sendMessage("§7[SK] §cЗадержка не может быть отрицательной");
            return true;
        }
        kit.setCooldown(seconds);
        plugin.getKits().flush(kit);
        sender.sendMessage("§7[SK] §aЗадержка набора §e" + kit.getName() + " §aустановлена на §c" + seconds + "s");
        return true;
    }

    private boolean setFlag(CommandSender sender, String[] args, boolean oneTimeUse) {
        String name = oneTimeUse ? "setonetimeuse" : "setfirstjoinkit";
        if (args.length != 3) {
            return usage(sender, "/sk " + name + " <кит> <true:false>");
        }
        Kit kit = require(sender, args[1]);
        if (kit == null) {
            return true;
        }
        if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false")) {
            return usage(sender, "/sk " + name + " <кит> <true:false>");
        }
        boolean value = Boolean.parseBoolean(args[2]);
        if (oneTimeUse) {
            kit.setOneTimeUse(value);
        } else {
            kit.setFirstTimeJoinKit(value);
        }
        plugin.getKits().flush(kit);
        sender.sendMessage("§7[SK] §aПараметр §e" + (oneTimeUse ? "OneTimeUse" : "FirstJoinKit")
                + " §aнабора §e" + kit.getName() + " §aустановлен на §c" + value);
        return true;
    }

    private boolean setIcon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLang().getPlayersOnly());
            return true;
        }
        if (args.length != 2) {
            return usage(sender, "/sk seticon <кит>");
        }
        Kit kit = require(sender, args[1]);
        if (kit == null) {
            return true;
        }
        Player player = (Player) sender;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            sender.sendMessage("§7[SK] §cВозьмите предмет в руку");
            return true;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            sender.sendMessage("§7[SK] §cОшибка, у этой иконки нет имени");
            return true;
        }
        for (Kit other : plugin.getKits().all()) {
            if (other != kit && hand.isSimilar(other.getIcon())) {
                sender.sendMessage("§7[SK] §cОшибка, это уже значок набора §e" + other.getName());
                return true;
            }
        }
        kit.setIcon(hand);
        plugin.getKits().flush(kit);
        sender.sendMessage("§7[SK] §aВы успешно установили значок набора §e" + kit.getName());
        return true;
    }

    private boolean setPermission(CommandSender sender, String[] args) {
        if (args.length != 3) {
            return usage(sender, "/sk setperm <кит> <право>");
        }
        Kit kit = require(sender, args[1]);
        if (kit == null) {
            return true;
        }
        kit.setPermission(args[2]);
        plugin.getKits().flush(kit);
        sender.sendMessage("§7[SK] §aПраво набора §e" + kit.getName() + " §aустановлено на §e" + args[2]);
        return true;
    }

    private boolean purge(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return usage(sender, "/sk purge <кит>");
        }
        Kit kit = require(sender, args[1]);
        if (kit == null) {
            return true;
        }
        if (kit.isOneTimeUse() || kit.getCooldown() <= 0L) {
            sender.sendMessage("§7[SK] §cУ этого набора нет истекающих кулдаунов");
            return true;
        }
        plugin.getPlayers().purgeExpired(kit.getKey(), kit.getCooldownMillis());
        sender.sendMessage("§7[SK] §aОчистка истёкших кулдаунов набора §e" + kit.getName() + " §aзапущена");
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
                List<String> suggestion = new ArrayList<>(1);
                suggestion.add(kit == null ? "strictkits.kits.kit" : kit.getPermission());
                return PlayerCommands.filter(suggestion, args[2]);
            }
        }
        return Collections.emptyList();
    }
}
