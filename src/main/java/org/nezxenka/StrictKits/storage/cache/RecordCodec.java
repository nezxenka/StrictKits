package org.nezxenka.StrictKits.storage.cache;

import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RecordCodec {

    private static final char SECTION = (char) 1;
    private static final char ENTRY = (char) 2;
    private static final char FIELD = (char) 3;

    private RecordCodec() {
    }

    public static String encode(PlayerRecord record) {
        for (String key : record.getCooldowns().keySet()) {
            if (!isSafe(key)) {
                return null;
            }
        }
        for (String claim : record.getClaims()) {
            if (!isSafe(claim)) {
                return null;
            }
        }

        StringBuilder builder = new StringBuilder(128);
        boolean first = true;
        for (Map.Entry<String, Long> entry : record.getCooldowns().entrySet()) {
            if (!first) {
                builder.append(ENTRY);
            }
            builder.append(entry.getKey()).append(FIELD).append(entry.getValue().longValue());
            first = false;
        }
        builder.append(SECTION);
        first = true;
        for (String claim : record.getClaims()) {
            if (!first) {
                builder.append(ENTRY);
            }
            builder.append(claim);
            first = false;
        }
        return builder.toString();
    }

    private static boolean isSafe(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == SECTION || c == ENTRY || c == FIELD) {
                return false;
            }
        }
        return true;
    }

    public static PlayerRecord decode(UUID uuid, String data) {
        if (data == null) {
            return null;
        }
        int section = data.indexOf(SECTION);
        if (section < 0) {
            return null;
        }
        Map<String, Long> cooldowns = new HashMap<>(8);
        Set<String> claims = new HashSet<>(8);

        int cursor = 0;
        while (cursor < section) {
            int next = data.indexOf(ENTRY, cursor);
            int end = (next < 0 || next > section) ? section : next;
            int field = data.indexOf(FIELD, cursor);
            if (field > cursor && field < end) {
                try {
                    cooldowns.put(data.substring(cursor, field), Long.parseLong(data.substring(field + 1, end)));
                } catch (NumberFormatException ignored) {
                }
            }
            cursor = end + 1;
        }

        cursor = section + 1;
        int length = data.length();
        while (cursor < length) {
            int next = data.indexOf(ENTRY, cursor);
            int end = next < 0 ? length : next;
            if (end > cursor) {
                claims.add(data.substring(cursor, end));
            }
            cursor = end + 1;
        }
        return new PlayerRecord(uuid, cooldowns, claims);
    }
}
