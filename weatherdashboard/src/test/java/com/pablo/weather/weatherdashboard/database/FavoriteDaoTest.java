package com.pablo.weather.weatherdashboard.database;

import com.pablo.weather.weatherdashboard.model.FavoriteLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FavoriteDaoTest {

    private static final String DATABASE_URL_PROPERTY =
            "weather.dashboard.database.url";

    @TempDir
    Path temporaryDirectory;

    private FavoriteDao favoriteDao;

    @BeforeEach
    void createTemporaryDatabase() {
        String databasePath = temporaryDirectory
                .resolve("favorites-test.db")
                .toAbsolutePath()
                .toString()
                .replace('\\', '/');

        System.setProperty(
                DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + databasePath
        );
        Database.initialize();
        favoriteDao = new FavoriteDao();
    }

    @AfterEach
    void restoreNormalDatabaseSetting() {
        System.clearProperty(DATABASE_URL_PROPERTY);
    }

    @Test
    void addsAndReadsFavorite() throws Exception {
        favoriteDao.addFavorite("33172");

        List<FavoriteLocation> favorites =
                favoriteDao.getAllFavorites();

        assertEquals(1, favorites.size());
        assertEquals("33172", favorites.getFirst().zipCode());
        assertEquals("33172", favorites.getFirst().nickname());
    }

    @Test
    void rejectsDuplicateFavorite() throws Exception {
        favoriteDao.addFavorite("33172");

        assertThrows(
                IllegalArgumentException.class,
                () -> favoriteDao.addFavorite("33172")
        );
    }

    @Test
    void updatesFavoriteNickname() throws Exception {
        favoriteDao.addFavorite("33172");
        favoriteDao.updateNickname("33172", "Home");

        assertEquals(
                "Home",
                favoriteDao.getAllFavorites().getFirst().nickname()
        );
    }

    @Test
    void updatesFavoriteZipCode() throws Exception {
        favoriteDao.addFavorite("33172");
        favoriteDao.updateFavorite("33172", "33174");

        FavoriteLocation updated =
                favoriteDao.getAllFavorites().getFirst();
        assertEquals("33174", updated.zipCode());
        assertEquals("33174", updated.nickname());
    }

    @Test
    void deletesOneFavoriteAndResetsAll() throws Exception {
        favoriteDao.addFavorite("33172");
        favoriteDao.addFavorite("33174");

        favoriteDao.deleteFavorite("33172");
        assertEquals(1, favoriteDao.countFavorites());

        favoriteDao.deleteAllFavorites();
        assertEquals(0, favoriteDao.countFavorites());
    }

    @Test
    void preventsMoreThanFiveFavorites() throws Exception {
        favoriteDao.addFavorite("10001");
        favoriteDao.addFavorite("10002");
        favoriteDao.addFavorite("10003");
        favoriteDao.addFavorite("10004");
        favoriteDao.addFavorite("10005");

        assertThrows(
                IllegalStateException.class,
                () -> favoriteDao.addFavorite("10006")
        );
        assertEquals(5, favoriteDao.countFavorites());
    }
}
