package org.nezxenka.strictkits.storage.sql.sqlite;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqliteSchema {

    public static String journalMode(String raw) {
        return "PRAGMA journal_mode=" + pragma(raw, "WAL");
    }

    public static String synchronous(String raw) {
        return "PRAGMA synchronous=" + pragma(raw, "NORMAL");
    }

    public static String createTable(String table, String timeColumn) {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid TEXT NOT NULL, "
                + "kit TEXT NOT NULL, "
                + timeColumn + " INTEGER NOT NULL, "
                + "PRIMARY KEY (uuid, kit))";
    }

    public static String createKitIndex(String table) {
        return "CREATE INDEX IF NOT EXISTS idx_" + table + "_kit ON " + table + " (kit)";
    }

    public static String upsert(String table, String timeColumn) {
        return "INSERT OR REPLACE INTO " + table + " (uuid, kit, " + timeColumn + ") VALUES (?, ?, ?)";
    }

    private static String pragma(String raw, String fallback) {
        String value = raw == null ? "" : raw.replaceAll("[^A-Za-z_]", "");
        return value.isEmpty() ? fallback : value;
    }
}
