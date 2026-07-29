package com.pablo.weather.weatherdashboard;

import com.pablo.weather.weatherdashboard.database.FavoriteDao;
import com.pablo.weather.weatherdashboard.database.SettingsDao;
import com.pablo.weather.weatherdashboard.model.DailyForecast;
import com.pablo.weather.weatherdashboard.model.FavoriteLocation;
import com.pablo.weather.weatherdashboard.model.HourlyForecast;
import com.pablo.weather.weatherdashboard.model.WeatherData;
import com.pablo.weather.weatherdashboard.service.WeatherService;
import com.pablo.weather.weatherdashboard.util.UnitConverter;
import com.pablo.weather.weatherdashboard.util.ZipCodeValidator;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Circle;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class WeatherDashboardController {

    private static final int HOURLY_PAGE_SIZE = 7;
    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd MMM, yyyy \u2022 h:mm a"
            );

    private static final String FOG_ICON_PATH = """
            M7 1H14A1 1 0 0 1 14 3H7A1 1 0 0 1 7 1Z
            M2 5H8A1 1 0 0 1 8 7H2A1 1 0 0 1 2 5Z
            M11 5H16A1 1 0 0 1 16 7H11A1 1 0 0 1 11 5Z
            M19 5H23A1 1 0 0 1 23 7H19A1 1 0 0 1 19 5Z
            M0 9H24A1 1 0 0 1 24 11H0A1 1 0 0 1 0 9Z
            M0 13H12A1 1 0 0 1 12 15H0A1 1 0 0 1 0 13Z
            M15 13H24A1 1 0 0 1 24 15H15A1 1 0 0 1 15 13Z
            M1 17H9A1 1 0 0 1 9 19H1A1 1 0 0 1 1 17Z
            M11 17H24A1 1 0 0 1 24 19H11A1 1 0 0 1 11 17Z
            M5 21H21A1 1 0 0 1 21 23H5A1 1 0 0 1 5 21Z
            """;
    private final FavoriteDao favoriteDao = new FavoriteDao();
    private final SettingsDao settingsDao = new SettingsDao();
    private final WeatherService weatherService = new WeatherService();
    private String currentZipCode;
    private String selectedFavoriteZipCode;
    private WeatherData currentWeather;
    private LocalDateTime lastWeatherUpdate;
    private Tooltip connectionStatusTooltip;
    private boolean useCelsius;
    private boolean useKilometersPerHour;
    private boolean lightMode;
    private boolean loadLastZipOnStartup;
    private boolean hourlyForecastVisible;
    private int hourlyPageIndex;
    private Timeline dateTimeTimeline;
    private boolean weatherSearchBusy;

    @FXML
    private Label dashboardTitle;

    @FXML
    private BorderPane dashboardRoot;

    @FXML
    private VBox dashboardContent;

    @FXML
    private VBox sidebar;

    @FXML
    private Button homeButton;

    @FXML
    private Label currentWeatherDayLabel;

    @FXML
    private Label currentWeatherDateTimeLabel;

    @FXML
    private Label liveBadge;

    @FXML
    private Button refreshWeatherButton;

    @FXML
    private StackPane weatherLoadingOverlay;

    @FXML
    private Label locationLabel;

    @FXML
    private Label temperatureLabel;

    @FXML
    private Button unitToggleButton;

    @FXML
    private Button speedUnitToggleButton;

    @FXML
    private Label conditionLabel;

    @FXML
    private Label humidityLabel;

    @FXML
    private Label feelsLikeLabel;

    @FXML
    private Label windSpeedLabel;

    @FXML
    private Label uvIndexLabel;

    @FXML
    private Label sunriseLabel;

    @FXML
    private Label sunsetLabel;

    @FXML
    private SVGPath weatherIcon;

    @FXML
    private Label locationDetailLabel;

    @FXML
    private HBox searchContainer;

    @FXML
    private TextField zipCodeField;

    @FXML
    private Button searchButton;

    @FXML
    private Label favoritesCounter;

    @FXML
    private VBox favoritesPane;

    @FXML
    private HBox forecastPane;

    @FXML
    private Label forecastTitle;

    @FXML
    private Button dailyForecastButton;

    @FXML
    private Button hourlyForecastButton;

    @FXML
    private void initialize() {
        setupConnectionStatusTooltip();
        loadSavedTheme();
        loadSavedUnits();
        loadSavedStartupPreference();
        startDateTimeUpdates();
        setupSidebarEntrance();
        setupFavoriteSelectionTracking();
        searchButton.setOpacity(1.0);
        setConnectionStatus(false);
        animateLiveBadge();
        refreshFavorites();
        setHomeNavigationSelected(true);
        Platform.runLater(this::loadLastViewedZipIfEnabled);
    }

    private void setupConnectionStatusTooltip() {
        connectionStatusTooltip = new Tooltip(
                "No successful weather update yet"
        );
        connectionStatusTooltip.getStyleClass().add(
                "status-info-tooltip"
        );
        connectionStatusTooltip.setShowDelay(
                Duration.millis(250)
        );
        connectionStatusTooltip.setHideDelay(
                Duration.millis(120)
        );
        Tooltip.install(liveBadge, connectionStatusTooltip);
    }

    private void startDateTimeUpdates() {
        dateTimeTimeline = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        event -> updateDateTimeDisplay()
                ),
                new KeyFrame(
                        Duration.seconds(1),
                        event -> updateDateTimeDisplay()
                )
        );
        dateTimeTimeline.setCycleCount(Animation.INDEFINITE);
        dateTimeTimeline.play();
    }

    private void updateDateTimeDisplay() {
        LocalDateTime now = LocalDateTime.now();
        currentWeatherDayLabel.setText(
                now.format(DAY_FORMAT)
        );
        currentWeatherDateTimeLabel.setText(
                now.format(DATE_TIME_FORMAT)
        );
        updateGreeting(now.getHour());
    }

    private void updateGreeting(int hour) {
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }
        dashboardTitle.setText(greeting);
    }

    private void setupSidebarEntrance() {
        sidebar.setOpacity(0);
        sidebar.setTranslateX(-82);

        Platform.runLater(() -> {
            FadeTransition fade = new FadeTransition(
                    Duration.millis(420),
                    sidebar
            );
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(
                    Duration.millis(420),
                    sidebar
            );
            slide.setFromX(-82);
            slide.setToX(0);

            new ParallelTransition(fade, slide).play();
        });
    }

    private void setupFavoriteSelectionTracking() {
        zipCodeField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    String enteredZipCode = newValue.trim();
                    if (selectedFavoriteZipCode != null
                            && !enteredZipCode.isEmpty()
                            && !enteredZipCode.equals(
                                    selectedFavoriteZipCode
                            )) {
                        selectedFavoriteZipCode = null;
                        refreshFavorites();
                    }
                }
        );
    }

    private void animateLiveBadge() {
        FadeTransition pulse = new FadeTransition(
                Duration.millis(900),
                liveBadge
        );
        pulse.setFromValue(0.55);
        pulse.setToValue(1.0);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    @FXML
    private void searchWeather() {
        if (weatherSearchBusy) {
            return;
        }

        String zipCode = zipCodeField.getText().trim();

        if (!isValidZipCode(zipCode)) {
            shakeSearchBar();
            return;
        }

        setWeatherSearchBusy(true);

        Task<WeatherData> weatherTask = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                return weatherService.getCurrentWeather(zipCode);
            }
        };

        weatherTask.setOnSucceeded(event -> {
            displayWeather(weatherTask.getValue());
            setWeatherSearchBusy(false);
        });

        weatherTask.setOnFailed(event -> {
            setWeatherSearchBusy(false);
            setConnectionStatus(false);
            shakeSearchBar();
            showMessage(
                    Alert.AlertType.ERROR,
                    "Weather search failed",
                    getErrorMessage(weatherTask.getException())
            );
        });

        Thread weatherThread = new Thread(
                weatherTask,
                "weather-api-request"
        );
        weatherThread.setDaemon(true);
        weatherThread.start();
    }

    @FXML
    private void refreshCurrentWeather() {
        if (currentZipCode == null) {
            return;
        }

        RotateTransition spin = new RotateTransition(
                Duration.millis(480),
                refreshWeatherButton.getGraphic()
        );
        spin.setByAngle(360);
        spin.play();

        zipCodeField.setText(currentZipCode);
        searchWeather();
    }

    @FXML
    private void showHelp() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Weather Dashboard");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setHeaderText("Help & Weather Guide");
        dialogPane.getStyleClass().add("help-dialog");
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        Label informationIcon = new Label("i");
        informationIcon.getStyleClass().add("dialog-information-icon");
        dialogPane.setGraphic(informationIcon);

        Tab instructionsTab = new Tab(
                "How to Use",
                createInstructionsPage()
        );
        instructionsTab.setClosable(false);

        Tab iconGuideTab = new Tab(
                "Weather Icons",
                createIconGuidePage()
        );
        iconGuideTab.setClosable(false);

        Tab weatherTermsTab = new Tab(
                "Weather Terms",
                createWeatherTermsPage()
        );
        weatherTermsTab.setClosable(false);

        TabPane pages = new TabPane(
                instructionsTab,
                iconGuideTab,
                weatherTermsTab
        );
        pages.getStyleClass().add("help-pages");
        pages.setPrefSize(520, 360);

        dialogPane.setContent(pages);
        styleDialog(dialogPane);
        prepareMovableDialog(dialog);
        dialog.showAndWait();
    }

    @FXML
    private void showAbout() {
        setHomeNavigationSelected(false);
        ScrollPane aboutPage = createAboutPage();
        switchMainPage(aboutPage);
    }

    @FXML
    private void showDashboard() {
        setHomeNavigationSelected(true);
        switchMainPage(dashboardContent);
    }

    private void setHomeNavigationSelected(boolean selected) {
        if (selected) {
            if (!homeButton.getStyleClass().contains(
                    "active-nav"
            )) {
                homeButton.getStyleClass().add("active-nav");
            }
        } else {
            homeButton.getStyleClass().remove("active-nav");
        }
    }

    private void switchMainPage(Node page) {
        if (dashboardRoot.getCenter() == page) {
            return;
        }

        page.setOpacity(1);
        page.setTranslateX(0);
        dashboardRoot.setCenter(page);
    }

    @FXML
    private void showSettings() {
        boolean savedLightMode = lightMode;
        boolean savedLoadLastZipOnStartup =
                loadLastZipOnStartup;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Settings");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setHeaderText("Settings");
        dialogPane.getStyleClass().add("settings-dialog");
        ButtonType applyButtonType = new ButtonType(
                "Apply",
                ButtonBar.ButtonData.APPLY
        );
        dialogPane.getButtonTypes().addAll(
                applyButtonType,
                ButtonType.CLOSE
        );

        Label settingLabel = new Label("Light mode");
        settingLabel.getStyleClass().add("settings-label");

        Circle switchKnob = new Circle(9);
        switchKnob.getStyleClass().add("theme-switch-knob");
        switchKnob.setTranslateX(lightMode ? 9 : -9);

        ToggleButton lightModeToggle = new ToggleButton();
        lightModeToggle.setGraphic(switchKnob);
        lightModeToggle.setSelected(lightMode);
        lightModeToggle.getStyleClass().add("theme-switch");
        lightModeToggle.selectedProperty().addListener(
                (observable, oldValue, selected) -> {
                    animateThemeSwitch(switchKnob, selected);
                    setLightMode(selected);
                    if (selected) {
                        if (!dialogPane.getStyleClass().contains("light-mode")) {
                            dialogPane.getStyleClass().add("light-mode");
                        }
                    } else {
                        dialogPane.getStyleClass().remove("light-mode");
                    }
                }
        );

        Region settingSpacer = new Region();
        HBox.setHgrow(settingSpacer, Priority.ALWAYS);

        HBox lightModeRow = new HBox(
                14,
                settingLabel,
                settingSpacer,
                lightModeToggle
        );
        lightModeRow.setAlignment(Pos.CENTER_LEFT);
        lightModeRow.getStyleClass().add("settings-row");

        Label startupZipLabel = new Label(
                "Load last viewed ZIP"
        );
        startupZipLabel.getStyleClass().add("settings-label");

        Circle startupZipSwitchKnob = new Circle(9);
        startupZipSwitchKnob.getStyleClass().add(
                "theme-switch-knob"
        );
        startupZipSwitchKnob.setTranslateX(
                loadLastZipOnStartup ? 9 : -9
        );

        ToggleButton startupZipToggle = new ToggleButton();
        startupZipToggle.setGraphic(startupZipSwitchKnob);
        startupZipToggle.setSelected(loadLastZipOnStartup);
        startupZipToggle.getStyleClass().add("theme-switch");
        startupZipToggle.selectedProperty().addListener(
                (observable, oldValue, selected) ->
                        animateThemeSwitch(
                                startupZipSwitchKnob,
                                selected
                        )
        );

        Region startupZipSpacer = new Region();
        HBox.setHgrow(startupZipSpacer, Priority.ALWAYS);

        HBox startupZipRow = new HBox(
                14,
                startupZipLabel,
                startupZipSpacer,
                startupZipToggle
        );
        startupZipRow.setAlignment(Pos.CENTER_LEFT);
        startupZipRow.getStyleClass().add("settings-row");

        Label favoritesLabel = new Label("Favorite ZIP codes");
        favoritesLabel.getStyleClass().add("settings-label");

        Region favoritesSpacer = new Region();
        HBox.setHgrow(favoritesSpacer, Priority.ALWAYS);

        Button resetFavoritesButton = new Button("Reset");
        resetFavoritesButton.getStyleClass().add("settings-reset-button");

        StackPane resetAction = new StackPane(resetFavoritesButton);
        resetAction.setMinWidth(72);
        resetAction.setPrefWidth(72);
        resetAction.setMaxWidth(72);
        resetFavoritesButton.setOnAction(
                event -> showResetConfirmation(
                        resetAction,
                        resetFavoritesButton
                )
        );

        HBox favoritesRow = new HBox(
                14,
                favoritesLabel,
                favoritesSpacer,
                resetAction
        );
        favoritesRow.setAlignment(Pos.CENTER_LEFT);
        favoritesRow.getStyleClass().add("settings-row");

        VBox settings = new VBox(
                12,
                lightModeRow,
                startupZipRow,
                favoritesRow
        );
        settings.setPadding(new Insets(20));
        settings.getStyleClass().add("settings-page");

        dialogPane.setContent(settings);
        styleDialog(dialogPane);
        dialogPane.lookupButton(applyButtonType)
                .getStyleClass()
                .add("settings-apply-button");
        prepareMovableDialog(dialog);

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CLOSE);
        if (result == applyButtonType) {
            try {
                settingsDao.saveLightMode(lightMode);
                settingsDao.saveLoadLastZipOnStartupEnabled(
                        startupZipToggle.isSelected()
                );
                loadLastZipOnStartup =
                        startupZipToggle.isSelected();
                if (
                        loadLastZipOnStartup
                                && currentZipCode != null
                                && isValidZipCode(currentZipCode)
                ) {
                    settingsDao.saveLastViewedZip(
                            currentZipCode
                    );
                }
            } catch (SQLException exception) {
                setLightMode(savedLightMode);
                loadLastZipOnStartup =
                        savedLoadLastZipOnStartup;
                showMessage(
                        Alert.AlertType.ERROR,
                        "Settings not saved",
                        "The selected settings could not be saved."
                );
            }
        } else {
            setLightMode(savedLightMode);
            loadLastZipOnStartup =
                    savedLoadLastZipOnStartup;
        }
    }

    private void loadSavedTheme() {
        try {
            setLightMode(settingsDao.isLightModeEnabled());
        } catch (SQLException exception) {
            setLightMode(false);
            System.err.println(
                    "Could not load saved theme: "
                            + exception.getMessage()
            );
        }
    }

    private void loadSavedUnits() {
        try {
            useCelsius = settingsDao.isCelsiusEnabled();
            useKilometersPerHour =
                    settingsDao.isKilometersPerHourEnabled();
        } catch (SQLException exception) {
            useCelsius = false;
            useKilometersPerHour = false;
            System.err.println(
                    "Could not load saved measurement units: "
                            + exception.getMessage()
            );
        }
        updateUnitToggleLabels();
    }

    private void loadSavedStartupPreference() {
        try {
            loadLastZipOnStartup =
                    settingsDao
                            .isLoadLastZipOnStartupEnabled();
        } catch (SQLException exception) {
            loadLastZipOnStartup = false;
            System.err.println(
                    "Could not load startup preference: "
                            + exception.getMessage()
            );
        }
    }

    private void loadLastViewedZipIfEnabled() {
        if (!loadLastZipOnStartup) {
            return;
        }

        try {
            String savedZipCode =
                    settingsDao.getLastViewedZip();
            if (
                    savedZipCode != null
                            && isValidZipCode(savedZipCode)
            ) {
                zipCodeField.setText(savedZipCode);
                searchWeather();
            }
        } catch (SQLException exception) {
            System.err.println(
                    "Could not load the last viewed ZIP code: "
                            + exception.getMessage()
            );
        }
    }

    private void rememberLastViewedZip() {
        if (
                !loadLastZipOnStartup
                        || currentZipCode == null
        ) {
            return;
        }

        try {
            settingsDao.saveLastViewedZip(currentZipCode);
        } catch (SQLException exception) {
            System.err.println(
                    "Could not save the last viewed ZIP code: "
                            + exception.getMessage()
            );
        }
    }

    private void showResetConfirmation(
            StackPane resetAction,
            Button resetButton
    ) {
        try {
            if (favoriteDao.countFavorites() == 0) {
                showTemporaryResetStatus(resetButton, "Empty");
                return;
            }
        } catch (SQLException exception) {
            showTemporaryResetStatus(resetButton, "Error");
            return;
        }

        SVGPath confirmIcon = new SVGPath();
        confirmIcon.setContent("M3 9L7 13L15 4");
        confirmIcon.getStyleClass().add("reset-confirm-icon");

        Button confirmButton = new Button();
        confirmButton.setGraphic(confirmIcon);
        confirmButton.setAccessibleText("Confirm reset");
        confirmButton.getStyleClass().addAll(
                "reset-choice-button",
                "reset-confirm-button"
        );

        SVGPath cancelIcon = new SVGPath();
        cancelIcon.setContent("M4 4L14 14M14 4L4 14");
        cancelIcon.getStyleClass().add("reset-cancel-icon");

        Button cancelButton = new Button();
        cancelButton.setGraphic(cancelIcon);
        cancelButton.setAccessibleText("Cancel reset");
        cancelButton.getStyleClass().addAll(
                "reset-choice-button",
                "reset-cancel-button"
        );

        HBox confirmation = new HBox(confirmButton, cancelButton);
        confirmation.setAlignment(Pos.CENTER);
        confirmation.getStyleClass().add("reset-confirmation");

        confirmButton.setOnAction(
                event -> confirmResetFavorites(
                        resetAction,
                        resetButton
                )
        );
        cancelButton.setOnAction(
                event -> swapResetControl(resetAction, resetButton)
        );

        swapResetControl(resetAction, confirmation);
    }

    private void confirmResetFavorites(
            StackPane resetAction,
            Button resetButton
    ) {
        try {
            favoriteDao.deleteAllFavorites();
            selectedFavoriteZipCode = null;
            refreshFavorites();
            resetButton.setText("\u2713 Done");
        } catch (SQLException exception) {
            resetButton.setText("Error");
        }

        swapResetControl(resetAction, resetButton);
        Timeline restoreText = new Timeline(
                new KeyFrame(
                        Duration.seconds(1.2),
                        event -> resetButton.setText("Reset")
                )
        );
        restoreText.play();
    }

    private void showTemporaryResetStatus(
            Button resetButton,
            String status
    ) {
        resetButton.setText(status);
        Timeline restoreText = new Timeline(
                new KeyFrame(
                        Duration.seconds(1.1),
                        event -> resetButton.setText("Reset")
                )
        );
        restoreText.play();
    }

    private void swapResetControl(
            StackPane resetAction,
            Node replacement
    ) {
        Node current = resetAction.getChildren().isEmpty()
                ? null
                : resetAction.getChildren().get(0);

        Runnable revealReplacement = () -> {
            replacement.setOpacity(0);
            replacement.setScaleX(0.82);
            replacement.setScaleY(0.82);
            resetAction.getChildren().setAll(replacement);

            FadeTransition fadeIn = new FadeTransition(
                    Duration.millis(150),
                    replacement
            );
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(
                    Duration.millis(150),
                    replacement
            );
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            new ParallelTransition(fadeIn, scaleIn).play();
        };

        if (current == null) {
            revealReplacement.run();
            return;
        }

        FadeTransition fadeOut = new FadeTransition(
                Duration.millis(100),
                current
        );
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> revealReplacement.run());
        fadeOut.play();
    }

    private void applySettingsWindowIcon(Dialog<?> dialog) {
        if (dialog.getDialogPane().getScene().getWindow() instanceof Stage stage) {
            stage.getIcons().setAll(createGearWindowIcon(64));
        }
    }

    private WritableImage createGearWindowIcon(int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pixels = image.getPixelWriter();
        double center = (size - 1) / 2.0;
        double outerRadius = size * 0.47;
        double bodyRadius = size * 0.38;
        double holeRadius = size * 0.14;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - center;
                double dy = y - center;
                double radius = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);
                double toothPosition = Math.abs(Math.cos(angle * 4));
                double edge = toothPosition > 0.68
                        ? outerRadius
                        : bodyRadius;

                if (radius <= edge && radius >= holeRadius) {
                    pixels.setColor(x, y, Color.rgb(22, 22, 22));
                } else {
                    pixels.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }

        return image;
    }

    private void animateThemeSwitch(
            Circle knob,
            boolean selected
    ) {
        TranslateTransition slide = new TranslateTransition(
                Duration.millis(170),
                knob
        );
        slide.setToX(selected ? 9 : -9);
        slide.play();
    }

    private void setLightMode(boolean enabled) {
        lightMode = enabled;

        if (enabled) {
            if (!dashboardRoot.getStyleClass().contains("light-mode")) {
                dashboardRoot.getStyleClass().add("light-mode");
            }
        } else {
            dashboardRoot.getStyleClass().remove("light-mode");
        }
    }

    private ScrollPane createInstructionsPage() {
        Label instructions = new Label("""
                1. Select the magnifying glass, enter a valid 5-digit U.S. ZIP code, then press Enter or select Search.

                2. View the current temperature, weather condition, humidity, feels-like temperature, wind speed, UV index, sunrise, and sunset.

                3. Use the 7 Day and Hourly buttons to change the forecast view. In Hourly view, use the left and right arrows to see more upcoming hours. Select the circular arrow beside the date and time to refresh the selected location.

                4. Use the \u00B0F \u21C4 button to switch between Fahrenheit and Celsius. Use the mph \u21C4 button beneath it to switch between miles and kilometers per hour. Both choices are saved automatically for the next time the application starts.

                5. Select Add Favorite to save the current ZIP code. You can save up to five favorite ZIP codes.

                6. Select a saved favorite to load its weather. Use the pencil to rename it, press Enter to save the name, or click elsewhere to cancel. Select \u00D7 to delete it.

                7. Open Settings with the gear button. You can preview Light mode, choose whether the last viewed ZIP loads at startup, select Apply to save your choices, or reset all favorite ZIP codes.

                8. A green LIVE badge means weather data loaded successfully. A red OFFLINE badge appears before a location is selected or when weather data cannot be loaded. Hover over either badge to see the last successful update time.

                9. The greeting, weekday, date, and time use the local date and time configured on your PC. They do not change to match the searched ZIP code.

                10. Use the ? button to reopen this guide. Weather Icons explains the symbols, and Weather Terms explains condition names such as Overcast and Drizzle. Select the weather logo at the top of the sidebar to view project information and credits.
                """);
        instructions.setWrapText(true);
        instructions.getStyleClass().add("help-copy");

        Label attribution = new Label(
                "Weather data provided by Open-Meteo"
        );
        attribution.getStyleClass().add("help-attribution");

        VBox page = new VBox(18, instructions, attribution);
        page.setPadding(new Insets(20));
        page.getStyleClass().add("help-page");

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("help-scroll");
        return scrollPane;
    }

    private ScrollPane createIconGuidePage() {
        VBox page = new VBox(
                10,
                createLegendRow("\u2600", "Clear", "Bright, mostly cloud-free conditions."),
                createLegendRow("\u2600\u2601", "Partly cloudy", "A mix of sunshine and clouds."),
                createLegendRow("\u2601", "Cloudy or overcast", "The sky is mostly or completely covered."),
                createLegendRow("\u2602", "Rain", "Drizzle, rain, or rain showers are expected."),
                createLegendRow("\u2744", "Snow", "Snowfall or snow showers are expected."),
                createLegendRow(
                        createFogIcon("legend-fog-icon"),
                        "Fog",
                        "Reduced visibility caused by fog or mist."
                ),
                createLegendRow("\u26A1", "Thunderstorm", "Storms with thunder and possible lightning.")
        );
        page.setPadding(new Insets(16));
        page.getStyleClass().add("help-page");

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("help-scroll");
        return scrollPane;
    }

    private ScrollPane createWeatherTermsPage() {
        VBox page = new VBox(
                10,
                createTermRow(
                        "Clear sky",
                        "The sky has little or no cloud cover."
                ),
                createTermRow(
                        "Mainly clear",
                        "Most of the sky is clear, with only a few clouds."
                ),
                createTermRow(
                        "Partly cloudy",
                        "Sunshine and clouds are both present."
                ),
                createTermRow(
                        "Overcast",
                        "Clouds cover nearly all or all of the sky."
                ),
                createTermRow(
                        "Fog",
                        "A cloud near the ground reduces how far you can see."
                ),
                createTermRow(
                        "Drizzle",
                        "Very light rain made of small water droplets."
                ),
                createTermRow(
                        "Freezing drizzle",
                        "Light rain that can freeze when it touches cold surfaces."
                ),
                createTermRow(
                        "Rain",
                        "Water droplets are falling steadily from clouds."
                ),
                createTermRow(
                        "Freezing rain",
                        "Rain that freezes on contact and may create slippery ice."
                ),
                createTermRow(
                        "Rain showers",
                        "Rain that begins and ends quickly or changes in intensity."
                ),
                createTermRow(
                        "Snow",
                        "Frozen flakes are falling from the sky."
                ),
                createTermRow(
                        "Snow showers",
                        "Short periods of snow that may start and stop suddenly."
                ),
                createTermRow(
                        "Thunderstorm",
                        "A storm with thunder and lightning, often with heavy rain."
                ),
                createTermRow(
                        "Unknown conditions",
                        "The weather service did not provide a recognized condition."
                )
        );
        page.setPadding(new Insets(16));
        page.getStyleClass().add("help-page");

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("help-scroll");
        return scrollPane;
    }

    private VBox createTermRow(String term, String meaning) {
        Label termLabel = new Label(term);
        termLabel.getStyleClass().add("legend-title");

        Label meaningLabel = new Label(meaning);
        meaningLabel.setWrapText(true);
        meaningLabel.getStyleClass().add("legend-description");

        VBox row = new VBox(3, termLabel, meaningLabel);
        row.getStyleClass().add("term-row");
        return row;
    }

    private ScrollPane createAboutPage() {
        Label heading = new Label("About");
        heading.getStyleClass().add("about-screen-heading");

        Label title = new Label("Weather Dashboard");
        title.getStyleClass().add("about-title");

        Label version = new Label("Version 1.0 — Mostly Sunny");
        version.getStyleClass().add("about-version");

        VBox page = new VBox(
                12,
                heading,
                title,
                version,
                createAboutDetail("Student", "Pablo Gallardo"),
                createAboutDetail(
                        "JavaFX",
                        "Desktop user interface and animations"
                ),
                createAboutDetail(
                        "SQLite",
                        "Favorite ZIP codes and saved settings"
                ),
                createAboutDetail(
                        "Open-Meteo",
                        "Current weather and forecast data"
                ),
                createAboutDetail(
                        "Zippopotam.us",
                        "U.S. ZIP code location lookup"
                )
        );
        page.getStyleClass().add("about-page");

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        scrollPane.getStyleClass().add("about-screen");
        return scrollPane;
    }

    private VBox createAboutDetail(
            String heading,
            String description
    ) {
        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("about-detail-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add(
                "about-detail-description"
        );

        VBox detail = new VBox(
                3,
                headingLabel,
                descriptionLabel
        );
        detail.getStyleClass().add("about-detail");
        return detail;
    }

    private HBox createLegendRow(
            String symbol,
            String title,
            String description
    ) {
        Label icon = new Label(symbol);
        icon.getStyleClass().add("legend-icon");
        return createLegendRow(icon, title, description);
    }

    private HBox createLegendRow(
            Node icon,
            String title,
            String description
    ) {
        StackPane iconHolder = new StackPane(icon);
        iconHolder.setMinWidth(58);
        iconHolder.setPrefWidth(58);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("legend-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("legend-description");
        descriptionLabel.setWrapText(true);

        VBox words = new VBox(
                2,
                titleLabel,
                descriptionLabel
        );
        HBox.setHgrow(words, Priority.ALWAYS);

        HBox row = new HBox(14, iconHolder, words);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("legend-row");
        return row;
    }

    private void displayWeather(WeatherData weather) {
        currentWeather = weather;
        currentZipCode = weather.zipCode();
        rememberLastViewedZip();
        hourlyPageIndex = 0;
        lastWeatherUpdate = LocalDateTime.now();
        updateConnectionStatusTooltip();
        setConnectionStatus(true);

        locationLabel.setText(
                weather.city() + ", " + weather.state()
        );
        locationDetailLabel.setText(
                "Current weather for "
                        + weather.city() + ", "
                        + weather.state() + " \u2022 "
                        + weather.zipCode()
        );
        conditionLabel.setText(weather.condition());
        humidityLabel.setText(weather.humidity() + "%");
        displayCurrentWindSpeed();
        uvIndexLabel.setText(
                String.format("%.1f", weather.uvIndex())
        );
        sunriseLabel.setText(weather.sunrise());
        sunsetLabel.setText(weather.sunset());
        updateWeatherIcon(weather.weatherCode());
        displayTemperatureValues();
        zipCodeField.clear();
    }

    private void setConnectionStatus(boolean live) {
        liveBadge.setText(live ? "LIVE" : "OFFLINE");
        liveBadge.getStyleClass().removeAll(
                "online-badge",
                "offline-badge"
        );
        liveBadge.getStyleClass().add(
                live ? "online-badge" : "offline-badge"
        );
    }

    private void updateConnectionStatusTooltip() {
        connectionStatusTooltip.setText(
                lastWeatherUpdate == null
                        ? "No successful weather update yet"
                        : "Last updated: "
                                + lastWeatherUpdate.format(
                                        DATE_TIME_FORMAT
                                )
        );
    }

    @FXML
    private void toggleTemperatureUnit() {
        useCelsius = !useCelsius;
        updateUnitToggleLabels();
        refreshTemperatureDisplay();

        try {
            settingsDao.saveCelsiusEnabled(useCelsius);
        } catch (SQLException exception) {
            useCelsius = !useCelsius;
            updateUnitToggleLabels();
            refreshTemperatureDisplay();
            showMessage(
                    Alert.AlertType.ERROR,
                    "Unit not saved",
                    "The temperature unit could not be saved."
            );
        }
    }

    @FXML
    private void toggleWindSpeedUnit() {
        useKilometersPerHour = !useKilometersPerHour;
        updateUnitToggleLabels();
        refreshWindSpeedDisplay();

        try {
            settingsDao.saveKilometersPerHourEnabled(
                    useKilometersPerHour
            );
        } catch (SQLException exception) {
            useKilometersPerHour = !useKilometersPerHour;
            updateUnitToggleLabels();
            refreshWindSpeedDisplay();
            showMessage(
                    Alert.AlertType.ERROR,
                    "Unit not saved",
                    "The wind-speed unit could not be saved."
            );
        }
    }

    private void updateUnitToggleLabels() {
        unitToggleButton.setText(
                (useCelsius ? "\u00B0C" : "\u00B0F")
                        + "  \u21C4"
        );
        speedUnitToggleButton.setText(
                (useKilometersPerHour ? "kph" : "mph")
                        + "  \u21C4"
        );
    }

    private void refreshTemperatureDisplay() {
        if (currentWeather != null) {
            displayTemperatureValues();
        }
    }

    private void refreshWindSpeedDisplay() {
        if (currentWeather == null) {
            return;
        }

        displayCurrentWindSpeed();
        if (hourlyForecastVisible) {
            displayHourlyForecast(
                    currentWeather.hourlyForecast()
            );
        }
    }

    private void displayCurrentWindSpeed() {
        windSpeedLabel.setText(
                String.format(
                        "%.1f %s",
                        convertWindSpeed(currentWeather.windSpeed()),
                        windSpeedUnit()
                )
        );
    }

    private double convertWindSpeed(double milesPerHour) {
        return useKilometersPerHour
                ? UnitConverter
                        .milesPerHourToKilometersPerHour(
                                milesPerHour
                        )
                : milesPerHour;
    }

    private String windSpeedUnit() {
        return useKilometersPerHour ? "kph" : "mph";
    }

    private void displayTemperatureValues() {
        temperatureLabel.setText(
                String.format(
                        "%.0f\u00B0",
                        convertTemperature(currentWeather.temperature())
                )
        );
        feelsLikeLabel.setText(
                String.format(
                        "%.0f\u00B0",
                        convertTemperature(currentWeather.feelsLike())
                )
        );
        refreshForecastView();
    }

    private double convertTemperature(double fahrenheit) {
        if (!useCelsius) {
            return fahrenheit;
        }

        return UnitConverter.fahrenheitToCelsius(fahrenheit);
    }

    @FXML
    private void showDailyForecast() {
        hourlyForecastVisible = false;
        hourlyPageIndex = 0;
        updateForecastSelector();
        if (currentWeather != null) {
            displayWeeklyForecast(currentWeather.weeklyForecast());
        }
    }

    @FXML
    private void showHourlyForecast() {
        hourlyForecastVisible = true;
        hourlyPageIndex = 0;
        updateForecastSelector();
        if (currentWeather != null) {
            displayHourlyForecast(currentWeather.hourlyForecast());
        }
    }

    private void refreshForecastView() {
        updateForecastSelector();
        if (hourlyForecastVisible) {
            displayHourlyForecast(currentWeather.hourlyForecast());
        } else {
            displayWeeklyForecast(currentWeather.weeklyForecast());
        }
    }

    private void updateForecastSelector() {
        forecastTitle.setText(
                hourlyForecastVisible
                        ? "Hourly Forecast"
                        : "7 Day Forecast"
        );
        dailyForecastButton.getStyleClass().remove(
                "forecast-view-button-selected"
        );
        hourlyForecastButton.getStyleClass().remove(
                "forecast-view-button-selected"
        );
        (hourlyForecastVisible
                ? hourlyForecastButton
                : dailyForecastButton
        ).getStyleClass().add("forecast-view-button-selected");
    }

    private void displayHourlyForecast(
            List<HourlyForecast> hourlyForecast
    ) {
        forecastPane.getChildren().clear();

        if (hourlyForecast.isEmpty()) {
            return;
        }

        int lastPage =
                (hourlyForecast.size() - 1) / HOURLY_PAGE_SIZE;
        hourlyPageIndex = Math.max(
                0,
                Math.min(hourlyPageIndex, lastPage)
        );

        Button previousButton = createHourlyPageButton(false);
        previousButton.setDisable(hourlyPageIndex == 0);

        Button nextButton = createHourlyPageButton(true);
        nextButton.setDisable(hourlyPageIndex == lastPage);

        int firstIndex = hourlyPageIndex * HOURLY_PAGE_SIZE;
        int lastIndex = Math.min(
                firstIndex + HOURLY_PAGE_SIZE,
                hourlyForecast.size()
        );

        HBox pageCards = new HBox(8);
        pageCards.setAlignment(Pos.CENTER);
        pageCards.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pageCards, Priority.ALWAYS);

        for (HourlyForecast hour
                : hourlyForecast.subList(firstIndex, lastIndex)) {
            Label timeLabel = new Label(hour.time());
            timeLabel.getStyleClass().add("forecast-day");

            Node symbolNode;
            if (hour.weatherCode() == 45
                    || hour.weatherCode() == 48) {
                symbolNode = createFogIcon("forecast-fog-symbol");
            } else {
                Label symbolLabel = new Label(
                        forecastSymbol(hour.weatherCode())
                );
                symbolLabel.getStyleClass().add("forecast-symbol");
                symbolNode = symbolLabel;
            }

            Label temperature = new Label(
                    String.format(
                            "%.0f\u00B0",
                            convertTemperature(hour.temperature())
                    )
            );
            temperature.getStyleClass().add("forecast-high");

            Label wind = new Label(
                    String.format(
                            "%.0f %s",
                            convertWindSpeed(hour.windSpeed()),
                            windSpeedUnit()
                    )
            );
            wind.getStyleClass().add("hourly-wind");

            VBox card = new VBox(
                    8,
                    timeLabel,
                    symbolNode,
                    temperature,
                    wind
            );
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("forecast-card");
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            pageCards.getChildren().add(card);
        }

        forecastPane.getChildren().setAll(
                previousButton,
                pageCards,
                nextButton
        );
    }

    private Button createHourlyPageButton(boolean next) {
        SVGPath arrow = new SVGPath();
        arrow.setContent(
                next
                        ? "M3 2L9 8L3 14"
                        : "M9 2L3 8L9 14"
        );
        arrow.getStyleClass().add("hourly-page-arrow");

        Button button = new Button();
        button.setGraphic(arrow);
        button.setAccessibleText(
                next
                        ? "Show later hours"
                        : "Show earlier hours"
        );
        button.getStyleClass().add("hourly-page-button");
        button.setOnAction(event ->
                changeHourlyPage(next ? 1 : -1)
        );
        return button;
    }

    private void changeHourlyPage(int direction) {
        if (currentWeather == null || !hourlyForecastVisible) {
            return;
        }

        List<HourlyForecast> hours =
                currentWeather.hourlyForecast();
        int lastPage = Math.max(
                0,
                (hours.size() - 1) / HOURLY_PAGE_SIZE
        );
        int requestedPage = Math.max(
                0,
                Math.min(hourlyPageIndex + direction, lastPage)
        );
        if (requestedPage == hourlyPageIndex) {
            return;
        }

        hourlyPageIndex = requestedPage;
        displayHourlyForecast(hours);
    }

    private void displayWeeklyForecast(
            List<DailyForecast> weeklyForecast
    ) {
        forecastPane.getChildren().clear();

        for (DailyForecast day : weeklyForecast) {
            Label dayLabel = new Label(day.day());
            dayLabel.getStyleClass().add("forecast-day");

            Node symbolNode;
            if (day.weatherCode() == 45
                    || day.weatherCode() == 48) {
                symbolNode = createFogIcon("forecast-fog-symbol");
            } else {
                Label symbolLabel = new Label(
                        forecastSymbol(day.weatherCode())
                );
                symbolLabel.getStyleClass().add("forecast-symbol");
                symbolNode = symbolLabel;
            }

            Label highLabel = new Label(
                    String.format(
                            "%.0f\u00B0",
                            convertTemperature(day.high())
                    )
            );
            highLabel.getStyleClass().add("forecast-high");

            Label lowLabel = new Label(
                    String.format(
                            "%.0f\u00B0",
                            convertTemperature(day.low())
                    )
            );
            lowLabel.getStyleClass().add("forecast-low");

            VBox card = new VBox(
                    8,
                    dayLabel,
                    symbolNode,
                    highLabel,
                    lowLabel
            );
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("forecast-card");
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            forecastPane.getChildren().add(card);
        }
    }

    private String forecastSymbol(int code) {
        if (code == 0 || code == 1) {
            return "\u2600";
        }
        if (code == 2) {
            return "\u2600\u2601";
        }
        if ((code >= 51 && code <= 67)
                || (code >= 80 && code <= 82)) {
            return "\u2602";
        }
        if ((code >= 71 && code <= 77)
                || code == 85 || code == 86) {
            return "\u2744";
        }
        if (code >= 95) {
            return "\u26A1";
        }
        return "\u2601";
    }

    private SVGPath createFogIcon(String styleClass) {
        SVGPath fogIcon = new SVGPath();
        fogIcon.setContent(FOG_ICON_PATH);
        fogIcon.getStyleClass().add(styleClass);
        return fogIcon;
    }

    private void updateWeatherIcon(int weatherCode) {
        String iconPath;

        if (weatherCode == 0 || weatherCode == 1) {
            iconPath = """
                    M6.76 4.84L4.96 3.05L3.55 4.46L5.34 6.25L6.76 4.84Z
                    M1 10.5H4V12.5H1V10.5Z
                    M11 1H13V4H11V1Z
                    M17.24 4.84L19.04 3.05L20.45 4.46L18.66 6.25L17.24 4.84Z
                    M20 10.5H23V12.5H20V10.5Z
                    M17.24 18.16L18.66 16.75L20.45 18.54L19.04 19.95L17.24 18.16Z
                    M11 19H13V22H11V19Z
                    M3.55 18.54L5.34 16.75L6.76 18.16L4.96 19.95L3.55 18.54Z
                    M12 6A6 6 0 1 0 12 18A6 6 0 1 0 12 6Z
                    """;
        } else if (weatherCode == 45 || weatherCode == 48) {
            iconPath = FOG_ICON_PATH;
        } else if ((weatherCode >= 51 && weatherCode <= 67)
                || (weatherCode >= 80 && weatherCode <= 82)) {
            iconPath = """
                    M18.5 9.5C17.8 6.4 15.1 4 11.8 4C9.2 4 6.9 5.5 5.8 7.8
                    C2.9 8.1 1 10.5 1 13.3C1 16.3 3.4 18.7 6.4 18.7H18.4
                    C21 18.7 23 16.6 23 14.1C23 11.6 21 9.6 18.5 9.5Z
                    M6.5 20H8.5L7.5 24H5.5L6.5 20Z
                    M11.5 20H13.5L12.5 24H10.5L11.5 20Z
                    M16.5 20H18.5L17.5 24H15.5L16.5 20Z
                    """;
        } else if ((weatherCode >= 71 && weatherCode <= 77)
                || weatherCode == 85 || weatherCode == 86) {
            iconPath = """
                    M18.5 8.5C17.8 5.8 15.2 4 12.2 4C9.6 4 7.3 5.4 6.1 7.6
                    C3.1 7.9 1 10.3 1 13.2C1 16.1 3.4 18.5 6.4 18.5H18.4
                    C21 18.5 23 16.4 23 13.9C23 11.3 21 9.3 18.5 8.5Z
                    M6 21A1.5 1.5 0 1 0 6 24A1.5 1.5 0 1 0 6 21Z
                    M12 20A1.5 1.5 0 1 0 12 23A1.5 1.5 0 1 0 12 20Z
                    M18 21A1.5 1.5 0 1 0 18 24A1.5 1.5 0 1 0 18 21Z
                    """;
        } else if (weatherCode >= 95) {
            iconPath = """
                    M18.5 8.5C17.8 5.8 15.2 4 12.2 4C9.6 4 7.3 5.4 6.1 7.6
                    C3.1 7.9 1 10.3 1 13.2C1 16.1 3.4 18.5 6.4 18.5H18.4
                    C21 18.5 23 16.4 23 13.9C23 11.3 21 9.3 18.5 8.5Z
                    M12.5 15H18L14 20H17L10 26L12 20H8L12.5 15Z
                    """;
        } else {
            iconPath = """
                    M19.35 10.04C18.67 6.59 15.64 4 12 4C9.11 4 6.6 5.64
                    5.35 8.04C2.34 8.36 0 10.9 0 14C0 17.31 2.69 20 6 20H19
                    C21.76 20 24 17.76 24 15C24 12.36 21.95 10.22 19.35 10.04Z
                    """;
        }

        String normalizedPath = iconPath.replaceAll("\\s+", " ").trim();
        weatherIcon.setContent(normalizedPath);
        animateWeatherIcon();
    }

    private void animateWeatherIcon() {
        weatherIcon.setOpacity(0);
        weatherIcon.setScaleX(1.7);
        weatherIcon.setScaleY(1.7);

        FadeTransition fade = new FadeTransition(
                Duration.millis(300),
                weatherIcon
        );
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(
                Duration.millis(300),
                weatherIcon
        );
        scale.setToX(2.4);
        scale.setToY(2.4);

        new ParallelTransition(fade, scale).play();
    }

    private void setWeatherSearchBusy(boolean busy) {
        weatherSearchBusy = busy;
        searchButton.setDisable(busy);
        zipCodeField.setDisable(busy);
        refreshWeatherButton.setDisable(
                busy || currentZipCode == null
        );
        searchButton.setText(busy ? "Loading..." : "Search");
        weatherLoadingOverlay.setManaged(busy);
        weatherLoadingOverlay.setVisible(busy);
    }

    private String getErrorMessage(Throwable exception) {
        Throwable cause = exception;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return "The weather request was interrupted.";
        }

        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return "Could not contact the weather service.";
        }

        return message;
    }

    private void saveFavorite() {
        String enteredZipCode = zipCodeField.getText().trim();
        String zipCode = enteredZipCode.isEmpty()
                ? currentZipCode
                : enteredZipCode;

        if (zipCode == null || !isValidZipCode(zipCode)) {
            shakeSearchBar();
            showMessage(
                    Alert.AlertType.WARNING,
                    "Invalid ZIP code",
                    "Enter a valid 5-digit ZIP code before saving."
            );
            return;
        }

        try {
            favoriteDao.addFavorite(zipCode);
            refreshFavorites();
            currentZipCode = zipCode;
            zipCodeField.clear();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "Favorite not saved",
                    exception.getMessage()
            );
        } catch (SQLException exception) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Database error",
                    "The favorite could not be saved."
            );
        }
    }

    private void deleteFavorite(String zipCode) {
        ButtonType deleteButtonType = new ButtonType(
                "Delete",
                ButtonBar.ButtonData.OK_DONE
        );
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete " + zipCode + " from favorites?",
                ButtonType.CANCEL,
                deleteButtonType
        );

        confirmation.initStyle(StageStyle.UNDECORATED);
        confirmation.setHeaderText("Delete favorite?");
        confirmation.setGraphic(null);
        confirmation.getDialogPane()
                .getStyleClass()
                .add("delete-favorite-dialog");
        styleDialog(confirmation);
        confirmation.getDialogPane()
                .lookupButton(deleteButtonType)
                .getStyleClass()
                .add("confirm-delete-button");
        prepareMovableDialog(confirmation);

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL)
                != deleteButtonType) {
            return;
        }

        try {
            favoriteDao.deleteFavorite(zipCode);
            if (zipCode.equals(selectedFavoriteZipCode)) {
                selectedFavoriteZipCode = null;
            }
            refreshFavorites();
        } catch (SQLException exception) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Database error",
                    "The favorite could not be deleted."
            );
        }
    }

    private void openFavorite(String zipCode) {
        selectedFavoriteZipCode = zipCode;
        refreshFavorites();
        zipCodeField.setText(zipCode);
        searchWeather();
    }

    private void beginInlineRename(
            FavoriteLocation favorite,
            StackPane nameContainer,
            VBox originalName
    ) {
        TextField editField = new TextField();
        editField.setPromptText("Rename");
        editField.setPrefColumnCount(8);
        editField.getStyleClass().add("favorite-name-editor");

        boolean[] finished = {false};
        nameContainer.getChildren().setAll(editField);

        editField.setOnAction(event -> {
            String nickname = editField.getText().trim();

            if (nickname.isEmpty() || nickname.length() > 16) {
                editField.getStyleClass().add(
                        "favorite-name-editor-invalid"
                );
                return;
            }

            finished[0] = true;
            try {
                favoriteDao.updateNickname(
                        favorite.zipCode(),
                        nickname
                );
                refreshFavorites();
            } catch (SQLException exception) {
                nameContainer.getChildren().setAll(originalName);
                showMessage(
                        Alert.AlertType.ERROR,
                        "Database error",
                        "The favorite name could not be updated."
                );
            }
        });

        editField.focusedProperty().addListener(
                (observable, wasFocused, isFocused) -> {
                    if (!isFocused && !finished[0]) {
                        nameContainer.getChildren().setAll(originalName);
                    }
                }
        );

        Platform.runLater(editField::requestFocus);
    }

    private void refreshFavorites() {
        try {
            List<FavoriteLocation> favorites =
                    favoriteDao.getAllFavorites();

            favoritesPane.getChildren().clear();
            favoritesCounter.setText(favorites.size() + " / 5");

            if (favorites.size() < 5) {
                Button addButton = new Button("+\nAdd Favorite");
                addButton.setWrapText(true);
                addButton.getStyleClass().addAll(
                        "favorite-card",
                        "favorite-action"
                );
                addButton.setOnAction(event -> saveFavorite());
                favoritesPane.getChildren().add(addButton);
            }

            for (FavoriteLocation favorite : favorites) {
                favoritesPane.getChildren().add(
                        createFavoriteCard(favorite)
                );
            }
        } catch (SQLException exception) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "Database error",
                    "Favorites could not be loaded."
            );
        }
    }

    private HBox createFavoriteCard(FavoriteLocation favorite) {
        Label nameLabel = new Label(displayName(favorite));
        nameLabel.getStyleClass().add("favorite-zip");

        VBox favoriteName = new VBox(nameLabel);
        favoriteName.setAlignment(Pos.CENTER_LEFT);

        if (!displayName(favorite).equals(favorite.zipCode())) {
            Label zipLabel = new Label(favorite.zipCode());
            zipLabel.getStyleClass().add("favorite-zip-detail");
            favoriteName.getChildren().add(zipLabel);
        }

        StackPane nameContainer = new StackPane(favoriteName);
        nameContainer.setAlignment(Pos.CENTER_LEFT);
        nameContainer.getStyleClass().add("favorite-selectable");
        nameContainer.setOnMouseClicked(event -> {
            if (!(nameContainer.getChildren().get(0)
                    instanceof TextField)) {
                openFavorite(favorite.zipCode());
            }
        });
        Button editButton = new Button("\u270E");
        editButton.setAccessibleText("Edit favorite name");
        editButton.getStyleClass().addAll(
                "mini-button",
                "edit-favorite-button"
        );
        editButton.setOnAction(
                event -> beginInlineRename(
                        favorite,
                        nameContainer,
                        favoriteName
                )
        );

        Button deleteButton = new Button("\u00D7");
        deleteButton.getStyleClass().addAll(
                "mini-button",
                "delete-button"
        );
        deleteButton.setOnAction(
                event -> deleteFavorite(favorite.zipCode())
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox card = new HBox(
                7,
                nameContainer,
                spacer,
                editButton,
                deleteButton
        );
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("favorite-card");
        if (favorite.zipCode().equals(selectedFavoriteZipCode)) {
            card.getStyleClass().add("favorite-card-selected");
        }
        return card;
    }

    private String displayName(FavoriteLocation favorite) {
        String nickname = favorite.nickname();
        return nickname == null || nickname.isBlank()
                ? favorite.zipCode()
                : nickname;
    }

    private boolean isValidZipCode(String zipCode) {
        return ZipCodeValidator.isValid(zipCode);
    }

    private void showMessage(
            Alert.AlertType type,
            String title,
            String message
    ) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane()
                .getStyleClass()
                .add("message-dialog");
        styleDialog(alert);
        prepareMovableDialog(alert);
        alert.showAndWait();
    }

    private void styleDialog(Alert alert) {
        styleDialog(alert.getDialogPane());
    }

    private void styleDialog(DialogPane dialogPane) {
        dialogPane.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("styles.css"),
                        "styles.css was not found"
                ).toExternalForm()
        );
        dialogPane.getStyleClass().add("weather-dialog");
        if (lightMode) {
            dialogPane.getStyleClass().add("light-mode");
        }
    }

    private void prepareMovableDialog(Dialog<?> dialog) {
        if (dashboardRoot.getScene() != null
                && dashboardRoot.getScene().getWindow() != null) {
            dialog.initOwner(
                    dashboardRoot.getScene().getWindow()
            );
        }

        dialog.setOnShown(event -> {
            if (!(dialog.getDialogPane().getScene().getWindow()
                    instanceof Stage dialogStage)) {
                return;
            }

            Node header = dialog.getDialogPane().lookup(
                    ".header-panel"
            );
            if (header == null) {
                return;
            }

            double[] dragOffset = new double[2];
            header.setOnMousePressed(mouseEvent -> {
                dragOffset[0] =
                        mouseEvent.getScreenX() - dialogStage.getX();
                dragOffset[1] =
                        mouseEvent.getScreenY() - dialogStage.getY();
            });

            header.setOnMouseDragged(mouseEvent -> {
                if (!(dashboardRoot.getScene().getWindow()
                        instanceof Stage ownerStage)) {
                    return;
                }

                double requestedX =
                        mouseEvent.getScreenX() - dragOffset[0];
                double requestedY =
                        mouseEvent.getScreenY() - dragOffset[1];

                double minimumX = ownerStage.getX();
                double minimumY = ownerStage.getY();
                double maximumX = Math.max(
                        minimumX,
                        ownerStage.getX()
                                + ownerStage.getWidth()
                                - dialogStage.getWidth()
                );
                double maximumY = Math.max(
                        minimumY,
                        ownerStage.getY()
                                + ownerStage.getHeight()
                                - dialogStage.getHeight()
                );

                dialogStage.setX(
                        Math.max(
                                minimumX,
                                Math.min(requestedX, maximumX)
                        )
                );
                dialogStage.setY(
                        Math.max(
                                minimumY,
                                Math.min(requestedY, maximumY)
                        )
                );
            });
        });
    }

    private void shakeSearchBar() {
        Timeline shake = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(
                                searchContainer.translateXProperty(),
                                0
                        )
                ),
                new KeyFrame(
                        Duration.millis(65),
                        new KeyValue(
                                searchContainer.translateXProperty(),
                                -8,
                                Interpolator.EASE_BOTH
                        )
                ),
                new KeyFrame(
                        Duration.millis(130),
                        new KeyValue(
                                searchContainer.translateXProperty(),
                                7,
                                Interpolator.EASE_BOTH
                        )
                ),
                new KeyFrame(
                        Duration.millis(200),
                        new KeyValue(
                                searchContainer.translateXProperty(),
                                -5,
                                Interpolator.EASE_BOTH
                        )
                ),
                new KeyFrame(
                        Duration.millis(270),
                        new KeyValue(
                                searchContainer.translateXProperty(),
                                3,
                                Interpolator.EASE_BOTH
                        )
                ),
                new KeyFrame(
                        Duration.millis(350),
                        new KeyValue(
                                searchContainer.translateXProperty(),
                                0,
                                Interpolator.EASE_OUT
                        )
                )
        );
        shake.play();
    }

}
