package org.nezxenka.StrictKits.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.nezxenka.StrictKits.util.Text;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Message {

    private static final int[] NO_SLOTS = new int[0];

    private final String[] literals;
    private final int[] slots;

    public static Message compile(String raw, String... keys) {
        String text = Text.color(raw);
        List<String> parts = new ArrayList<>(keys.length + 1);
        List<Integer> found = new ArrayList<>(keys.length);
        int cursor = 0;
        while (true) {
            int key = -1;
            int at = -1;
            for (int i = 0; i < keys.length; i++) {
                int index = text.indexOf(keys[i], cursor);
                if (index >= 0 && (key < 0 || index < at || (index == at && keys[i].length() > keys[key].length()))) {
                    key = i;
                    at = index;
                }
            }
            if (key < 0) {
                break;
            }
            parts.add(text.substring(cursor, at));
            found.add(key);
            cursor = at + keys[key].length();
        }
        if (found.isEmpty()) {
            return new Message(new String[]{text}, NO_SLOTS);
        }
        parts.add(text.substring(cursor));
        return new Message(parts.toArray(String[]::new), found.stream().mapToInt(Integer::intValue).toArray());
    }

    public String format(Object... values) {
        if (slots.length == 0) {
            return literals[0];
        }
        StringBuilder builder = new StringBuilder(literals[0]);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] < values.length) {
                builder.append(values[slots[i]]);
            }
            builder.append(literals[i + 1]);
        }
        return builder.toString();
    }
}
