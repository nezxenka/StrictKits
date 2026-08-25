package org.nezxenka.StrictKits.util;

public final class TimeFormat {

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60L * SECOND;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;
    private static final long MONTH = 30L * DAY;
    private static final long YEAR = 365L * DAY;

    private TimeFormat() {
    }

    public static String getFormattedCooldown(long millis) {
        if (millis <= 0L) {
            return "0s";
        }

        long remaining = millis;
        long years = remaining / YEAR;
        remaining -= years * YEAR;
        long months = remaining / MONTH;
        remaining -= months * MONTH;
        long days = remaining / DAY;
        remaining -= days * DAY;
        long hours = remaining / HOUR;
        remaining -= hours * HOUR;
        long minutes = remaining / MINUTE;
        remaining -= minutes * MINUTE;
        long seconds = remaining / SECOND;

        StringBuilder builder = new StringBuilder(24);
        if (years > 0L) {
            builder.append(years).append('y');
        }
        if (builder.length() > 0 || months > 0L) {
            builder.append(months).append("mo");
        }
        if (builder.length() > 0 || days > 0L) {
            builder.append(days).append('d');
        }
        if (builder.length() > 0 || hours > 0L) {
            builder.append(hours).append('h');
        }
        if (builder.length() > 0 || minutes > 0L) {
            builder.append(minutes).append('m');
        }
        builder.append(seconds).append('s');
        return builder.toString();
    }
}
