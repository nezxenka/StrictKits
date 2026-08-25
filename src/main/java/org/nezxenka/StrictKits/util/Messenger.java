package org.nezxenka.StrictKits.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Messenger {

    private static volatile boolean placeholderApi;

    private Messenger() {
    }

    public static void detect() {
        placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static void send(CommandSender target, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (placeholderApi && target instanceof Player) {
            target.sendMessage(PlaceholderAPI.setPlaceholders((Player) target, message));
            return;
        }
        target.sendMessage(message);
    }

    public static void send(CommandSender target, String[] messages) {
        if (messages == null || messages.length == 0) {
            return;
        }
        if (placeholderApi && target instanceof Player) {
            Player player = (Player) target;
            String[] resolved = new String[messages.length];
            for (int i = 0; i < messages.length; i++) {
                resolved[i] = PlaceholderAPI.setPlaceholders(player, messages[i]);
            }
            target.sendMessage(resolved);
            return;
        }
        target.sendMessage(messages);
    }
}
