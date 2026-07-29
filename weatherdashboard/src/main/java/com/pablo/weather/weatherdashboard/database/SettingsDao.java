package com.pablo.weather.weatherdashboard.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDao {

    private static final String LIGHT_MODE_KEY = "light_mode";
    private static final String CELSIUS_KEY = "use_celsius";
    private static final String KILOMETERS_PER_HOUR_KEY =
            "use_kilometers_per_hour";
    private static final String LOAD_LAST_ZIP_KEY =
            "load_last_zip_on_startup";
    private static final String LAST_VIEWED_ZIP_KEY =
            "last_viewed_zip";

    public boolean isLightModeEnabled() throws SQLException {
        return readBoolean(LIGHT_MODE_KEY);
    }

    public boolean isCelsiusEnabled() throws SQLException {
        return readBoolean(CELSIUS_KEY);
    }

    public boolean isKilometersPerHourEnabled()
            throws SQLException {
        return readBoolean(KILOMETERS_PER_HOUR_KEY);
    }

    public boolean isLoadLastZipOnStartupEnabled()
            throws SQLException {
        return readBoolean(LOAD_LAST_ZIP_KEY);
    }

    public String getLastViewedZip() throws SQLException {
        return readSetting(LAST_VIEWED_ZIP_KEY);
    }

    public void saveLightMode(boolean enabled) throws SQLException {
        saveBoolean(LIGHT_MODE_KEY, enabled);
    }

    public void saveCelsiusEnabled(boolean enabled)
            throws SQLException {
        saveBoolean(CELSIUS_KEY, enabled);
    }

    public void saveKilometersPerHourEnabled(boolean enabled)
            throws SQLException {
        saveBoolean(KILOMETERS_PER_HOUR_KEY, enabled);
    }

    public void saveLoadLastZipOnStartupEnabled(boolean enabled)
            throws SQLException {
        saveBoolean(LOAD_LAST_ZIP_KEY, enabled);
    }

    public void saveLastViewedZip(String zipCode)
            throws SQLException {
        saveSetting(LAST_VIEWED_ZIP_KEY, zipCode);
    }

    private boolean readBoolean(String settingKey)
            throws SQLException {
        return Boolean.parseBoolean(readSetting(settingKey));
    }

    private String readSetting(String settingKey)
            throws SQLException {
        String sql = """
                SELECT setting_value
                FROM application_settings
                WHERE setting_key = ?
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, settingKey);

            try (ResultSet results = statement.executeQuery()) {
                return results.next()
                        ? results.getString("setting_value")
                        : null;
            }
        }
    }

    private void saveBoolean(
            String settingKey,
            boolean enabled
    ) throws SQLException {
        saveSetting(settingKey, Boolean.toString(enabled));
    }

    private void saveSetting(
            String settingKey,
            String settingValue
    ) throws SQLException {
        String sql = """
                INSERT INTO application_settings (
                    setting_key,
                    setting_value
                )
                VALUES (?, ?)
                ON CONFLICT(setting_key)
                DO UPDATE SET setting_value = excluded.setting_value
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, settingKey);
            statement.setString(2, settingValue);
            statement.executeUpdate();
        }
    }
}
