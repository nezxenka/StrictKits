package org.nezxenka.strictkits.permission;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Permissions {

    public static final String ADMIN = "strictkits.admin";
    public static final String LIST = "strictkits.list";
    public static final String PREVIEW = "strictkits.preview";
    public static final String KIT_PREFIX = "strictkits.kits.";
    public static final String ALL_KITS = KIT_PREFIX + "*";

    public static String kit(String kitName) {
        return KIT_PREFIX + kitName;
    }
}
