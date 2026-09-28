package org.nezxenka.strictkits.storage.sql.mysql;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class MySqlCollationMigration {

    private static final String SELECT_KIT_COLLATION = "SELECT COLLATION_NAME FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'kit'";

    private final Logger logger;

    public void apply(Connection connection, List<String> tables) throws SQLException {
        for (String table : tables) {
            String collation = kitCollation(connection, table);
            if (collation != null && !MySqlSchema.KIT_COLLATION.equalsIgnoreCase(collation)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(MySqlSchema.alterKitColumn(table));
                }
                logger.info("Колонка kit в " + table + " переведена с " + collation + " на " + MySqlSchema.KIT_COLLATION);
            }
        }
    }

    private static String kitCollation(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_KIT_COLLATION)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }
}
