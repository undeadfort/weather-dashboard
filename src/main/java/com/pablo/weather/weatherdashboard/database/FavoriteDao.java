package com.pablo.weather.weatherdashboard.database;

import com.pablo.weather.weatherdashboard.model.FavoriteLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDao {

    private static final int MAXIMUM_FAVORITES = 5;

    public void addFavorite(String zipCode) throws SQLException {
        if (favoriteExists(zipCode)) {
            throw new IllegalArgumentException(
                    "That ZIP code is already a favorite."
            );
        }

        if (countFavorites() >= MAXIMUM_FAVORITES) {
            throw new IllegalStateException(
                    "You can only save five favorite ZIP codes."
            );
        }

        String sql = """
                INSERT INTO favorite_locations (
                    zip_code,
                    nickname
                )
                VALUES (?, ?)
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, zipCode);
            statement.setString(2, zipCode);
            statement.executeUpdate();
        }
    }

    public List<FavoriteLocation> getAllFavorites() throws SQLException {
        List<FavoriteLocation> favorites = new ArrayList<>();

        String sql = """
                SELECT zip_code, nickname
                FROM favorite_locations
                ORDER BY created_at
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                favorites.add(
                        new FavoriteLocation(
                                results.getString("zip_code"),
                                results.getString("nickname")
                        )
                );
            }
        }

        return favorites;
    }

    public void deleteFavorite(String zipCode) throws SQLException {
        String sql = """
                DELETE FROM favorite_locations
                WHERE zip_code = ?
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, zipCode);
            statement.executeUpdate();
        }
    }

    public void deleteAllFavorites() throws SQLException {
        String sql = "DELETE FROM favorite_locations";

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.executeUpdate();
        }
    }

    public void updateFavorite(
            String oldZipCode,
            String newZipCode
    ) throws SQLException {
        String sql = """
                UPDATE favorite_locations
                SET zip_code = ?, nickname = ?
                WHERE zip_code = ?
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, newZipCode);
            statement.setString(2, newZipCode);
            statement.setString(3, oldZipCode);
            statement.executeUpdate();
        }
    }

    public void updateNickname(
            String zipCode,
            String nickname
    ) throws SQLException {
        String sql = """
                UPDATE favorite_locations
                SET nickname = ?
                WHERE zip_code = ?
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, nickname);
            statement.setString(2, zipCode);
            statement.executeUpdate();
        }
    }

    public int countFavorites() throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM favorite_locations
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()
        ) {
            return results.getInt(1);
        }
    }

    private boolean favoriteExists(String zipCode)
            throws SQLException {

        String sql = """
                SELECT 1
                FROM favorite_locations
                WHERE zip_code = ?
                """;

        try (
                Connection connection = Database.connect();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, zipCode);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }
}
