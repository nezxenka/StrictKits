package org.nezxenka.StrictKits.config;

import org.nezxenka.StrictKits.util.Text;

import java.util.ArrayList;
import java.util.List;

public final class Message {

    private static final int[] NO_SLOTS = new int[0];

    private final String[] literals;
    private final int[] slots;
    private final int hint;

    private Message(String[] literals, int[] slots, int hint) {
        this.literals = literals;
        this.slots = slots;
        this.hint = hint;
    }

    public static Message compile(String raw, String... keys) {
        String text = Text.color(raw);
        if (keys.length == 0 || text.isEmpty()) {
            return new Message(new String[]{text}, NO_SLOTS, text.length());
        }

        List<String> parts = new ArrayList<>(keys.length + 1);
        List<Integer> found = new ArrayList<>(keys.length);
        int cursor = 0;

        while (true) {
            int key = -1;
            int at = Integer.MAX_VALUE;
            for (int i = 0; i < keys.length; i++) {
                int index = text.indexOf(keys[i], cursor);
                if (index < 0) {
                    continue;
                }
                if (index < at || (index == at && keys[i].length() > keys[key].length())) {
                    at = index;
                    key = i;
                }
            }
            if (key < 0) {
                break;
            }
            parts.add(text.substring(cursor, at));
            found.add(key);
            cursor = at + keys[key].length();
        }
        parts.add(text.substring(cursor));

        if (found.isEmpty()) {
            return new Message(new String[]{text}, NO_SLOTS, text.length());
        }

        String[] literals = parts.toArray(new String[0]);
        int[] slots = new int[found.size()];
        int hint = 0;
        for (int i = 0; i < slots.length; i++) {
            slots[i] = found.get(i);
        }
        for (int i = 0; i < literals.length; i++) {
            hint += literals[i].length();
        }
        return new Message(literals, slots, hint + slots.length * 12);
    }

    public boolean isEmpty() {
        return slots.length == 0 && literals[0].isEmpty();
    }

    public String value() {
        return literals[0];
    }

    public String format(String value) {
        if (slots.length == 0) {
            return literals[0];
        }
        StringBuilder builder = new StringBuilder(hint + value.length());
        builder.append(literals[0]);
        for (int i = 0; i < slots.length; i++) {
            builder.append(value).append(literals[i + 1]);
        }
        return builder.toString();
    }

    public String format(String first, String second) {
        if (slots.length == 0) {
            return literals[0];
        }
        StringBuilder builder = new StringBuilder(hint + first.length() + second.length());
        builder.append(literals[0]);
        for (int i = 0; i < slots.length; i++) {
            builder.append(slots[i] == 0 ? first : second).append(literals[i + 1]);
        }
        return builder.toString();
    }

    public String format(String... values) {
        if (slots.length == 0) {
            return literals[0];
        }
        StringBuilder builder = new StringBuilder(hint);
        builder.append(literals[0]);
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (slot < values.length) {
                builder.append(values[slot]);
            }
            builder.append(literals[i + 1]);
        }
        return builder.toString();
    }
}
