package org.nezxenka.StrictKits.storage.cache;

import lombok.experimental.UtilityClass;
import org.nezxenka.StrictKits.storage.PlayerRecord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class RecordCodec {

    private static final char SECTION = (char) 1;
    private static final char ENTRY = (char) 2;
    private static final char FIELD = (char) 3;
    private static final String ENTRY_SEPARATOR = String.valueOf(ENTRY);

    public static String encode(PlayerRecord record) {
        Map<String, Long> cooldowns = record.getCooldowns();
        Set<String> claims = record.getClaims();
        if (!Stream.concat(cooldowns.keySet().stream(), claims.stream()).allMatch(RecordCodec::isSafe)) {
            return null;
        }
        StringJoiner encodedCooldowns = new StringJoiner(ENTRY_SEPARATOR);
        cooldowns.forEach((kit, stamp) -> encodedCooldowns.add(kit + FIELD + stamp));
        return encodedCooldowns.toString() + SECTION + String.join(ENTRY_SEPARATOR, claims);
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
        entries(data.substring(0, section)).forEach(entry -> {
            int field = entry.indexOf(FIELD);
            if (field <= 0) {
                return;
            }
            try {
                cooldowns.put(entry.substring(0, field), Long.parseLong(entry.substring(field + 1)));
            } catch (NumberFormatException ignored) {
            }
        });
        Set<String> claims = entries(data.substring(section + 1)).collect(Collectors.toSet());
        return new PlayerRecord(uuid, cooldowns, claims);
    }

    private static Stream<String> entries(String section) {
        return Arrays.stream(section.split(ENTRY_SEPARATOR)).filter(entry -> !entry.isEmpty());
    }

    private static boolean isSafe(String value) {
        return value.indexOf(SECTION) < 0 && value.indexOf(ENTRY) < 0 && value.indexOf(FIELD) < 0;
    }
}
