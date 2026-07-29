package com.pablo.weather.weatherdashboard.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsDaoTest {

    private static final String DATABASE_URL_PROPERTY =
            "weather.dashboard.database.url";

    @TempDir
    Path temporaryDirectory;

    private SettingsDao settingsDao;

    @BeforeEach
    void createTemporaryDatabase() {
        String databasePath = temporaryDirectory
                .resolve("settings-test.db")
                .toAbsolutePath()
                .toString()
                .replace('\\', '/');

        System.setProperty(
                DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + databasePath
        );
        Database.initialize();
        settingsDao = new SettingsDao();
    }

    @AfterEach
    void restoreNormalDatabaseSetting() {
        System.clearProperty(DATABASE_URL_PROPERTY);
    }

    @Test
    void newDatabaseUsesDefaultSettings() throws Exception {
        assertFalse(settingsDao.isLightModeEnabled());
        assertFalse(settingsDao.isCelsiusEnabled());
        assertFalse(settingsDao.isKilometersPerHourEnabled());
        assertFalse(settingsDao.isLoadLastZipOnStartupEnabled());
        assertEquals(null, settingsDao.getLastViewedZip());
    }

    @Test
    void savesAndReadsApplicationSettings() throws Exception {
        settingsDao.saveLightMode(true);
        settingsDao.saveCelsiusEnabled(true);
        settingsDao.saveKilometersPerHourEnabled(true);
        settingsDao.saveLoadLastZipOnStartupEnabled(true);
        settingsDao.saveLastViewedZip("33172");

        assertTrue(settingsDao.isLightModeEnabled());
        assertTrue(settingsDao.isCelsiusEnabled());
        assertTrue(settingsDao.isKilometersPerHourEnabled());
        assertTrue(
                settingsDao.isLoadLastZipOnStartupEnabled()
        );
        assertEquals("33172", settingsDao.getLastViewedZip());
    }
}
