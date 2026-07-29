package com.pablo.weather.weatherdashboard.util;

public final class UnitConverter {

    private static final double KILOMETERS_PER_MILE =
            1.609344;

    private UnitConverter() {
    }

    public static double fahrenheitToCelsius(
            double fahrenheit
    ) {
        return (fahrenheit - 32.0) * 5.0 / 9.0;
    }

    public static double milesPerHourToKilometersPerHour(
            double milesPerHour
    ) {
        return milesPerHour * KILOMETERS_PER_MILE;
    }
}
