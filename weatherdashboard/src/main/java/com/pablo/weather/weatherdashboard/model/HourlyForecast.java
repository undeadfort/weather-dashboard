package com.pablo.weather.weatherdashboard.model;

public record HourlyForecast(
        String time,
        double temperature,
        double windSpeed,
        int weatherCode
) {
}
