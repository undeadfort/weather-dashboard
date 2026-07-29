package com.pablo.weather.weatherdashboard.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String DEFAULT_DATABASE_URL =
            "jdbc:sqlite:weather-dashboard.db";
    private static final String DATABASE_URL_PROPERTY =
            "weather.dashboard.database.url";

    private Database() {
    }

    public static Connection connect() throws SQLException {
        String databaseUrl = System.getProperty(
                DATABASE_URL_PROPERTY,
                DEFAULT_DATABASE_URL
        );
        return DriverManager.getConnection(databaseUrl);
    }

    public static void initialize() {
        String createFavoritesTable = """
                CREATE TABLE IF NOT EXISTS favorite_locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    zip_code TEXT NOT NULL UNIQUE,
                    nickname TEXT NOT NULL,
                    city TEXT,
                    state TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS application_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL
                )
                """;

        try (
                Connection connection = connect();
            Statement statement = connection.createStatement()
        ) {
            statement.execute(createFavoritesTable);
            statement.execute(createSettingsTable);

            System.out.println(
                    "SQLite database initialized successfully."
            );
        } catch (SQLException exception) {
            System.err.println(
                    "Could not initialize the database: "
                            + exception.getMessage()
            );
        }
    }
}
