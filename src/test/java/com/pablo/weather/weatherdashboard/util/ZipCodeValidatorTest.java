package com.pablo.weather.weatherdashboard.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipCodeValidatorTest {

    @Test
    void acceptsFiveDigitZipCode() {
        assertTrue(ZipCodeValidator.isValid("33172"));
    }

    @Test
    void rejectsZipCodeWithTooFewDigits() {
        assertFalse(ZipCodeValidator.isValid("123"));
    }

    @Test
    void rejectsLetters() {
        assertFalse(ZipCodeValidator.isValid("ABCDE"));
    }

    @Test
    void rejectsEmptyAndNullValues() {
        assertFalse(ZipCodeValidator.isValid(""));
        assertFalse(ZipCodeValidator.isValid(null));
    }
}
