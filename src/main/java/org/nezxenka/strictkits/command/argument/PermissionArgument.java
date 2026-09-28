package org.nezxenka.strictkits.command.argument;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class PermissionArgument {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    public static boolean isValid(String permission) {
        return VALID.matcher(permission).matches();
    }
}
