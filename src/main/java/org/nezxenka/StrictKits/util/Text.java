package org.nezxenka.StrictKits.util;

import org.bukkit.ChatColor;

public final class Text {

    private Text() {
    }

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
