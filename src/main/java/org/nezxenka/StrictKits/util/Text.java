package org.nezxenka.StrictKits.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Text {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})|<#([A-Fa-f0-9]{6})>");

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String withHex = HEX.matcher(input).replaceAll(match ->
                Matcher.quoteReplacement(toLegacyHex(match.group(1) != null ? match.group(1) : match.group(2))));
        return ChatColor.translateAlternateColorCodes('&', withHex);
    }

    private static String toLegacyHex(String hex) {
        StringBuilder builder = new StringBuilder(14).append(ChatColor.COLOR_CHAR).append('x');
        for (char digit : hex.toCharArray()) {
            builder.append(ChatColor.COLOR_CHAR).append(digit);
        }
        return builder.toString();
    }
}
