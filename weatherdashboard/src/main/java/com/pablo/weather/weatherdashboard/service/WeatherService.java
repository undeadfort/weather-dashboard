package com.pablo.weather.weatherdashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablo.weather.weatherdashboard.model.DailyForecast;
import com.pablo.weather.weatherdashboard.model.HourlyForecast;
import com.pablo.weather.weatherdashboard.model.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WeatherService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherData getCurrentWeather(String zipCode)
            throws IOException, InterruptedException {

        JsonNode locationResponse = requestJson(
                "https://api.zippopotam.us/us/" + zipCode
        );

        JsonNode place = locationResponse.path("places").path(0);

        if (place.isMissingNode()) {
            throw new IOException(
                    "No location was found for ZIP code " + zipCode + "."
            );
        }

        String city = place.path("place name").asText();
        String state = place.path("state abbreviation").asText();
        double latitude = place.path("latitude").asDouble();
        double longitude = place.path("longitude").asDouble();

        String weatherUrl =
                "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=" + latitude
                        + "&longitude=" + longitude
                        + "&current=temperature_2m,"
                        + "relative_humidity_2m,"
                        + "apparent_temperature,"
                        + "weather_code,"
                        + "wind_speed_10m"
                        + "&daily=weather_code,"
                        + "temperature_2m_max,"
                        + "temperature_2m_min,"
                        + "sunrise,sunset,"
                        + "uv_index_max"
                        + "&hourly=temperature_2m,"
                        + "weather_code,"
                        + "wind_speed_10m"
                        + "&temperature_unit=fahrenheit"
                        + "&wind_speed_unit=mph"
                        + "&forecast_days=7"
                        + "&timezone=auto";

        JsonNode weatherResponse = requestJson(weatherUrl);
        JsonNode current = weatherResponse.path("current");
        JsonNode daily = weatherResponse.path("daily");
        JsonNode hourly = weatherResponse.path("hourly");

        if (current.isMissingNode()) {
            throw new IOException(
                    "The weather provider returned no current conditions."
            );
        }

        int weatherCode = current.path("weather_code").asInt();
        List<DailyForecast> weeklyForecast =
                parseWeeklyForecast(daily);
        List<HourlyForecast> hourlyForecast =
                parseHourlyForecast(
                        hourly,
                        current.path("time").asText()
                );

        return new WeatherData(
                zipCode,
                city,
                state,
                current.path("temperature_2m").asDouble(),
                current.path("apparent_temperature").asDouble(),
                current.path("relative_humidity_2m").asInt(),
                current.path("wind_speed_10m").asDouble(),
                weatherCode,
                describeWeatherCode(weatherCode),
                daily.path("uv_index_max").path(0).asDouble(),
                formatTime(daily.path("sunrise").path(0).asText()),
                formatTime(daily.path("sunset").path(0).asText()),
                weeklyForecast,
                hourlyForecast
        );
    }

    private List<HourlyForecast> parseHourlyForecast(
            JsonNode hourly,
            String currentTime
    ) {
        List<HourlyForecast> forecast = new ArrayList<>();
        JsonNode times = hourly.path("time");
        JsonNode temperatures = hourly.path("temperature_2m");
        JsonNode codes = hourly.path("weather_code");
        JsonNode windSpeeds = hourly.path("wind_speed_10m");

        int startIndex = 0;
        for (int index = 0; index < times.size(); index++) {
            if (times.path(index).asText().compareTo(currentTime) >= 0) {
                startIndex = index;
                break;
            }
        }

        for (int slot = 0; slot < 21; slot++) {
            int index = startIndex + slot;
            if (index >= times.size()) {
                break;
            }

            LocalDateTime time = LocalDateTime.parse(
                    times.path(index).asText()
            );

            forecast.add(new HourlyForecast(
                    slot == 0
                            ? "Now"
                            : time.format(
                                    DateTimeFormatter.ofPattern(
                                            slot < 7
                                                    ? "h a"
                                                    : "EEE h a"
                                    )
                            ),
                    temperatures.path(index).asDouble(),
                    windSpeeds.path(index).asDouble(),
                    codes.path(index).asInt()
            ));
        }

        return forecast;
    }

    private List<DailyForecast> parseWeeklyForecast(JsonNode daily) {
        List<DailyForecast> forecast = new ArrayList<>();
        JsonNode dates = daily.path("time");
        JsonNode highs = daily.path("temperature_2m_max");
        JsonNode lows = daily.path("temperature_2m_min");
        JsonNode codes = daily.path("weather_code");

        for (int index = 0; index < dates.size(); index++) {
            LocalDate date = LocalDate.parse(dates.path(index).asText());
            String day = index == 0
                    ? "Today"
                    : date.format(DateTimeFormatter.ofPattern("EEE"));

            forecast.add(new DailyForecast(
                    day,
                    highs.path(index).asDouble(),
                    lows.path(index).asDouble(),
                    codes.path(index).asInt()
            ));
        }

        return forecast;
    }

    private String formatTime(String value) {
        if (value == null || value.isBlank()) {
            return "--";
        }

        return LocalDateTime.parse(value)
                .format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    private JsonNode requestJson(String url)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            throw new IOException("That ZIP code was not found.");
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "The weather service returned HTTP "
                            + response.statusCode() + "."
            );
        }

        return objectMapper.readTree(response.body());
    }

    private String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown conditions";
        };
    }
}
