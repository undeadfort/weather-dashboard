package com.pablo.weather.weatherdashboard.model;

import java.util.List;

public record WeatherData(
        String zipCode,
        String city,
        String state,
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed,
        int weatherCode,
        String condition,
        double uvIndex,
        String sunrise,
        String sunset,
        List<DailyForecast> weeklyForecast,
        List<HourlyForecast> hourlyForecast
) {
}
