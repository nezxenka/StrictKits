package org.nezxenka.StrictKits.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.nezxenka.StrictKits.Main;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.kit.Kit;
import org.nezxenka.StrictKits.util.Completions;
import org.nezxenka.StrictKits.util.Messenger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public final class AdminCommands implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "remove", "setinv", "setcooldown", "setonetimeuse",
            "setfirstjoinkit", "seticon", "setperm", "purge", "reload", "stats", "version");
    private static final Set<String> WITHOUT_KIT_ARGUMENT = Set.of("create", "reload", "stats", "version");
    private static final Set<String> FLAG_SUBCOMMANDS = Set.of("setonetimeuse", "setfirstjoinkit");
    private static final Set<String> RESERVED_NAMES = Set.of("list", "preview", "help");
    private static final List<String> BOOLEANS = List.of("true", "false");
    private static final Pattern VALID_PERMISSION = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final Main plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length == 0) {
            Messenger.send(sender, messages.getAdminHelp());
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> create(sender, messages, args);
            case "remove" -> remove(sender, messages, args);
            case "setinv" -> setInventory(sender, messages, args);
            case "setcooldown" -> setCooldown(sender, messages, args);
            case "setonetimeuse" -> setFlag(sender, messages, args, "OneTimeUse", Kit::setOneTimeUse);
            case "setfirstjoinkit" -> setFlag(sender, messages, args, "FirstJoinKit", Kit::setFirstTimeJoinKit);
            case "seticon" -> setIcon(sender, messages, args);
            case "setperm" -> setPermission(sender, messages, args);
            case "purge" -> purge(sender, messages, args);
            case "reload" -> reload(sender);
            case "stats" -> Messenger.send(sender, messages.formatAdminStats(plugin.getStorage().name(),
                    plugin.getCache().name(), plugin.getKits().size(), plugin.getPlayers()));
            case "version" -> Messenger.send(sender, messages.getAdminVersion());
            default -> Messenger.send(sender, messages.getAdminHelp());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Completions.filter(SUBCOMMANDS, args[0]);
        }
        String subcommand = args[0].toLowerCase();
        if (args.length == 2 && !WITHOUT_KIT_ARGUMENT.contains(subcommand)) {
            return Completions.filter(plugin.getKits().names(), args[1]);
        }
        if (args.length == 3 && FLAG_SUBCOMMANDS.contains(subcommand)) {
            return Completions.filter(BOOLEANS, args[2]);
        }
        if (args.length == 3 && subcommand.equals("setperm")) {
            Kit kit = plugin.getKits().get(args[1]);
            String suggestion = kit == null ? Kit.PERMISSION_PREFIX + "kit" : kit.getPermission();
            return Completions.filter(List.of(suggestion), args[2]);
        }
        return Collections.emptyList();
    }

    private void create(CommandSender sender, Messages messages, String[] args) {
        if (args.length != 2) {
            sendUsage(sender, messages, args);
            return;
        }
        String name = args[1];
        if (!Kit.isValidName(name) || RESERVED_NAMES.contains(name.toLowerCase())) {
            Messenger.send(sender, messages.getAdminKitNameInvalid());
            return;
        }
        Kit kit = plugin.getKits().create(name);
        if (kit == null) {
            Messenger.send(sender, messages.getAdminKitExists());
            return;
        }
        Messenger.send(sender, messages.getAdminKitCreated().format(kit.getName()));
    }

    private void remove(CommandSender sender, Messages messages, String[] args) {
        Kit kit = findKit(sender, messages, args, 2);
        if (kit == null) {
            return;
        }
        plugin.getKits().remove(kit);
        plugin.getPlayers().onKitRemoved(kit.getKey());
        Messenger.send(sender, messages.getAdminKitRemoved().format(kit.getName()));
    }

    private void setInventory(CommandSender sender, Messages messages, String[] args) {
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        Kit kit = findKit(sender, messages, args, 2);
        if (kit == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] main = inventory.getContents();
        Arrays.fill(main, Kit.STORAGE_SIZE, Kit.STORAGE_SIZE + Kit.ARMOR_SIZE, null);
        kit.setMainContent(main);
        kit.setArmorContent(inventory.getArmorContents());
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminInventoryUpdated().format(kit.getName()));
    }

    private void setCooldown(CommandSender sender, Messages messages, String[] args) {
        Kit kit = findKit(sender, messages, args, 3);
        if (kit == null) {
            return;
        }
        long seconds;
        try {
            seconds = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            Messenger.send(sender, messages.getAdminCooldownNotANumber());
            return;
        }
        if (seconds < 0L) {
            Messenger.send(sender, messages.getAdminCooldownNegative());
            return;
        }
        if (seconds > Kit.MAX_COOLDOWN_SECONDS) {
            Messenger.send(sender, messages.getAdminCooldownTooLarge().format(Kit.MAX_COOLDOWN_SECONDS));
            return;
        }
        kit.setCooldown(seconds);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminCooldownUpdated().format(kit.getName(), seconds));
    }

    private void setFlag(CommandSender sender, Messages messages, String[] args, String flag,
                         BiConsumer<Kit, Boolean> setter) {
        Kit kit = findKit(sender, messages, args, 3);
        if (kit == null) {
            return;
        }
        if (!BOOLEANS.contains(args[2].toLowerCase())) {
            sendUsage(sender, messages, args);
            return;
        }
        boolean value = Boolean.parseBoolean(args[2]);
        setter.accept(kit, value);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminFlagUpdated().format(flag, kit.getName(), value));
    }

    private void setIcon(CommandSender sender, Messages messages, String[] args) {
        if (!(sender instanceof Player player)) {
            Messenger.send(sender, messages.getPlayersOnly());
            return;
        }
        Kit kit = findKit(sender, messages, args, 2);
        if (kit == null) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Messenger.send(sender, messages.getAdminIconHandEmpty());
            return;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            Messenger.send(sender, messages.getAdminIconWithoutName());
            return;
        }
        Optional<Kit> owner = plugin.getKits().all().stream()
                .filter(other -> other != kit && hand.isSimilar(other.getIcon()))
                .findFirst();
        if (owner.isPresent()) {
            Messenger.send(sender, messages.getAdminIconAlreadyUsed().format(owner.get().getName()));
            return;
        }
        kit.setIcon(hand);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminIconUpdated().format(kit.getName()));
    }

    private void setPermission(CommandSender sender, Messages messages, String[] args) {
        Kit kit = findKit(sender, messages, args, 3);
        if (kit == null) {
            return;
        }
        String permission = args[2].trim();
        if (!VALID_PERMISSION.matcher(permission).matches()) {
            Messenger.send(sender, messages.getAdminPermissionInvalid());
            return;
        }
        kit.setPermission(permission);
        plugin.getKits().flush(kit);
        Messenger.send(sender, messages.getAdminPermissionUpdated().format(kit.getName(), permission));
    }

    private void purge(CommandSender sender, Messages messages, String[] args) {
        Kit kit = findKit(sender, messages, args, 2);
        if (kit == null) {
            return;
        }
        if (kit.isOneTimeUse() || kit.getCooldown() <= 0L) {
            Messenger.send(sender, messages.getAdminPurgeNothing());
            return;
        }
        plugin.getPlayers().purgeExpired(kit.getKey(), kit.getCooldownMillis());
        Messenger.send(sender, messages.getAdminPurgeStarted().format(kit.getName()));
    }

    private void reload(CommandSender sender) {
        plugin.reloadPlugin();
        Messenger.send(sender, plugin.getMessages().getAdminReloaded().format(plugin.getKits().size()));
    }

    private Kit findKit(CommandSender sender, Messages messages, String[] args, int expectedLength) {
        if (args.length != expectedLength) {
            sendUsage(sender, messages, args);
            return null;
        }
        Kit kit = plugin.getKits().get(args[1]);
        if (kit == null) {
            Messenger.send(sender, messages.getAdminKitNotFound());
        }
        return kit;
    }

    private static void sendUsage(CommandSender sender, Messages messages, String[] args) {
        Messenger.send(sender, messages.getUsage(args[0].toLowerCase()));
    }
}
