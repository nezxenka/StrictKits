package org.nezxenka.strictkits.storage.sql.mysql;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MySqlSchema {

    public static final String KIT_COLLATION = "utf8mb4_bin";
    public static final String KIT_COLUMN = "kit VARCHAR(64) CHARACTER SET utf8mb4 COLLATE " + KIT_COLLATION + " NOT NULL";

    public static String createTable(String table, String timeColumn) {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid CHAR(36) NOT NULL, "
                + KIT_COLUMN + ", "
                + timeColumn + " BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, kit), "
                + "INDEX idx_kit (kit)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    }

    public static String upsert(String table, String timeColumn) {
        return "INSERT INTO " + table + " (uuid, kit, " + timeColumn + ") VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE " + timeColumn + " = VALUES(" + timeColumn + ")";
    }

    public static String alterKitColumn(String table) {
        return "ALTER TABLE " + table + " MODIFY " + KIT_COLUMN;
    }
}
