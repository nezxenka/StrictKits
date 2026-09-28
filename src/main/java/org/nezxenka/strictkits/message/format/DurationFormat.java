package org.nezxenka.strictkits.message.format;

import lombok.RequiredArgsConstructor;
import org.nezxenka.strictkits.message.template.MessageReader;

import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public final class DurationFormat {

    private static final String PATH = "time.";

    private final String separator;
    private final Map<Unit, String> suffixes = new EnumMap<>(Unit.class);

    public DurationFormat(MessageReader reader) {
        this.separator = reader.raw(PATH + "separator");
        for (Unit unit : Unit.values()) {
            suffixes.put(unit, reader.plain(PATH + unit.key));
        }
    }

    public String format(long millis) {
        long remaining = millis <= 0L ? 0L : TimeUnit.MILLISECONDS.toSeconds(millis + 999L);
        StringJoiner joiner = new StringJoiner(separator);
        for (Unit unit : Unit.values()) {
            long amount = remaining / unit.seconds;
            remaining %= unit.seconds;
            if (amount > 0L) {
                joiner.add(amount + suffixes.get(unit));
            }
        }
        return joiner.length() > 0 ? joiner.toString() : "0" + suffixes.get(Unit.SECONDS);
    }

    @RequiredArgsConstructor
    private enum Unit {
        YEARS("years", TimeUnit.DAYS.toSeconds(365L)),
        MONTHS("months", TimeUnit.DAYS.toSeconds(30L)),
        DAYS("days", TimeUnit.DAYS.toSeconds(1L)),
        HOURS("hours", TimeUnit.HOURS.toSeconds(1L)),
        MINUTES("minutes", TimeUnit.MINUTES.toSeconds(1L)),
        SECONDS("seconds", 1L);

        private final String key;
        private final long seconds;
    }
}
