package com.pablo.weather.weatherdashboard;

import com.pablo.weather.weatherdashboard.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class WeatherDashboardApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Database.initialize();

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        getClass().getResource("hello-view.fxml"),
                        "hello-view.fxml was not found"
                )
        );

        Parent dashboard = loader.load();

        Image icon = new Image(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("weather-icon.png"),
                        "weather-icon.png was not found"
                )
        );

        Scene scene = new Scene(dashboard, 1100, 730);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("styles.css"),
                        "styles.css was not found"
                ).toExternalForm()
        );

        stage.setTitle("Weather Dashboard");
        stage.getIcons().add(icon);
        stage.setMinWidth(800);
        stage.setMinHeight(630);
        stage.setScene(scene);
        stage.show();
    }
}
