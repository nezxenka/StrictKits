package org.nezxenka.StrictKits.util;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class TimeFormat {

    public static String format(long millis) {
        if (millis <= 0L) {
            return "0s";
        }
        StringBuilder builder = new StringBuilder(24);
        long remaining = millis;
        for (Unit unit : Unit.values()) {
            long amount = remaining / unit.millis;
            remaining %= unit.millis;
            if (amount > 0L || builder.length() > 0) {
                builder.append(amount).append(unit.suffix);
            }
        }
        return builder.append(TimeUnit.MILLISECONDS.toSeconds(remaining)).append('s').toString();
    }

    @RequiredArgsConstructor
    private enum Unit {
        YEAR(TimeUnit.DAYS.toMillis(365L), "y"),
        MONTH(TimeUnit.DAYS.toMillis(30L), "mo"),
        DAY(TimeUnit.DAYS.toMillis(1L), "d"),
        HOUR(TimeUnit.HOURS.toMillis(1L), "h"),
        MINUTE(TimeUnit.MINUTES.toMillis(1L), "m");

        private final long millis;
        private final String suffix;
    }
}
