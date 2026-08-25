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
}
