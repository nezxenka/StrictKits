package org.nezxenka.StrictKits.config;

import org.nezxenka.StrictKits.util.Text;

import java.util.ArrayList;
import java.util.List;

public final class Message {

    private static final int[] NO_SLOTS = new int[0];

    private final String[] literals;
    private final int[] slots;

    private Message(String[] literals, int[] slots) {
        this.literals = literals;
        this.slots = slots;
    }

    public static Message compile(String raw, String... keys) {
        String text = Text.color(raw);
        if (keys.length == 0 || text.isEmpty()) {
            return new Message(new String[]{text}, NO_SLOTS);
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
                if (index < at || (key >= 0 && index == at && keys[i].length() > keys[key].length())) {
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
            return new Message(new String[]{text}, NO_SLOTS);
        }

        int[] slots = new int[found.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = found.get(i);
        }
        return new Message(parts.toArray(new String[0]), slots);
    }

    public String format(String... values) {
        if (slots.length == 0) {
            return literals[0];
        }
        StringBuilder builder = new StringBuilder();
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
