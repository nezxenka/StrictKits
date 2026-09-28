package org.nezxenka.strictkits.kit.naming;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class KitNames {

    public static final int MAX_LENGTH = 32;

    private static final Pattern VALID = Pattern.compile("[\\p{L}\\p{Nd}_-]{1," + MAX_LENGTH + "}");
    private static final Set<String> RESERVED = Set.of("list", "preview", "help");

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    public static boolean isReserved(String name) {
        return RESERVED.contains(key(name));
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
