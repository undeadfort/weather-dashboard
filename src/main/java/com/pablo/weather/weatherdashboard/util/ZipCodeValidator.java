package com.pablo.weather.weatherdashboard.util;

public final class ZipCodeValidator {

    private ZipCodeValidator() {
    }

    public static boolean isValid(String zipCode) {
        return zipCode != null && zipCode.matches("\\d{5}");
    }
}
