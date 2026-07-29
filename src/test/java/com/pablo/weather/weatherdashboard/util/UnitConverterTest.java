package com.pablo.weather.weatherdashboard.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitConverterTest {

    @Test
    void convertsFreezingPointToCelsius() {
        assertEquals(
                0.0,
                UnitConverter.fahrenheitToCelsius(32.0),
                0.001
        );
    }

    @Test
    void convertsBoilingPointToCelsius() {
        assertEquals(
                100.0,
                UnitConverter.fahrenheitToCelsius(212.0),
                0.001
        );
    }

    @Test
    void convertsMilesPerHourToKilometersPerHour() {
        assertEquals(
                16.09344,
                UnitConverter
                        .milesPerHourToKilometersPerHour(10.0),
                0.00001
        );
    }

    @Test
    void preservesZeroDuringConversions() {
        assertEquals(
                -17.7778,
                UnitConverter.fahrenheitToCelsius(0.0),
                0.0001
        );
        assertEquals(
                0.0,
                UnitConverter
                        .milesPerHourToKilometersPerHour(0.0),
                0.001
        );
    }

    @Test
    void negativeFortyIsTheSameInBothTemperatureScales() {
        assertEquals(
                -40.0,
                UnitConverter.fahrenheitToCelsius(-40.0),
                0.001
        );
    }

    @Test
    void convertsOneMilePerHour() {
        assertEquals(
                1.609344,
                UnitConverter
                        .milesPerHourToKilometersPerHour(1.0),
                0.000001
        );
    }
}
