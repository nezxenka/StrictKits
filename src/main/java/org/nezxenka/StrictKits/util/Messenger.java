package org.nezxenka.StrictKits.util;

import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@UtilityClass
public class Messenger {

    private static volatile boolean placeholderApi;

    public static void detect() {
        placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static void send(CommandSender target, String message) {
        if (message != null && !message.isEmpty()) {
            target.sendMessage(resolve(target, message));
        }
    }

    public static void send(CommandSender target, String[] messages) {
        if (messages != null && messages.length > 0) {
            target.sendMessage(Arrays.stream(messages).map(line -> resolve(target, line)).toArray(String[]::new));
        }
    }

    private static String resolve(CommandSender target, String message) {
        return placeholderApi && target instanceof Player player
                ? PlaceholderAPI.setPlaceholders(player, message)
                : message;
    }
}
