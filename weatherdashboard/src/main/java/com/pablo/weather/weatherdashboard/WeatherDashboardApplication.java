package com.pablo.weather.weatherdashboard;

import com.pablo.weather.weatherdashboard.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

        Label title = new Label("Weather Dashboard");
        title.getStyleClass().add("window-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeButton = new Button("–");
        minimizeButton.getStyleClass().add("window-control-button");
        minimizeButton.setOnAction(event -> stage.setIconified(true));

        Button maximizeButton = new Button("□");
        maximizeButton.getStyleClass().add("window-control-button");
        maximizeButton.setOnAction(
                event -> stage.setMaximized(!stage.isMaximized())
        );

        Button closeButton = new Button("×");
        closeButton.getStyleClass().addAll(
                "window-control-button",
                "window-close-button"
        );
        closeButton.setOnAction(event -> stage.close());

        HBox windowControls = new HBox(
                minimizeButton,
                maximizeButton,
                closeButton
        );
        windowControls.setSpacing(0);
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        HBox titleBar = new HBox(
                title,
                spacer,
                windowControls
        );
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("custom-title-bar");

        double[] dragOffset = new double[2];
        titleBar.setOnMousePressed(event -> {
            if (!stage.isMaximized()) {
                dragOffset[0] = event.getSceneX();
                dragOffset[1] = event.getSceneY();
            }
        });
        titleBar.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - dragOffset[0]);
                stage.setY(event.getScreenY() - dragOffset[1]);
            }
        });
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                stage.setMaximized(!stage.isMaximized());
            }
        });

        VBox window = new VBox(titleBar, dashboard);
        VBox.setVgrow(dashboard, Priority.ALWAYS);
        window.getStyleClass().add("application-window");

        Scene scene = new Scene(window, 1100, 730);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("styles.css"),
                        "styles.css was not found"
                ).toExternalForm()
        );

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Weather Dashboard");
        stage.getIcons().add(icon);
        stage.setMinWidth(800);
        stage.setMinHeight(630);
        stage.setScene(scene);
        stage.show();
    }
}
