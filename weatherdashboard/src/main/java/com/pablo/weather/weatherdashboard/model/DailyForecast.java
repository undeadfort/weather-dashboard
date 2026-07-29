package com.pablo.weather.weatherdashboard.model;

public record DailyForecast(
        String day,
        double high,
        double low,
        int weatherCode
) {
}
