package org.nezxenka.StrictKits.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class Text {

    private Text() {
    }

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> color(List<String> input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(0);
        }
        List<String> out = new ArrayList<>(input.size());
        for (int i = 0; i < input.size(); i++) {
            out.add(color(input.get(i)));
        }
        return out;
    }

    public static String replace(String input, String key, String value) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int index = input.indexOf(key);
        if (index < 0) {
            return input;
        }
        StringBuilder builder = new StringBuilder(input.length() + value.length());
        int cursor = 0;
        while (index >= 0) {
            builder.append(input, cursor, index).append(value);
            cursor = index + key.length();
            index = input.indexOf(key, cursor);
        }
        builder.append(input, cursor, input.length());
        return builder.toString();
    }
}
