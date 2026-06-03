package weatherData.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataTest {

        private static final java.time.LocalDate DATE = java.time.LocalDate.of(2025, 6, 15);
    private static final java.time.LocalTime START = java.time.LocalTime.of(12, 0);
    private static final java.time.LocalTime END = java.time.LocalTime.of(13, 0);

    private WeatherData validWeatherData() {
        return new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 180.0, 15.0, DATE, START, END);
    }

    // --- Construction ---

    @Test
    void ensureWeatherDataWithValidDataIsCreated() {
        final WeatherData wd = validWeatherData();
        assertEquals("PT-NORTH", wd.acaCode());
        assertEquals(DATE, wd.date());
        assertEquals(180.0, wd.direction());
        assertEquals(15.0, wd.windSpeed());
        assertEquals(43.840454, wd.latitude1());
        assertEquals(-9.795711, wd.longitude1());
        assertNotNull(wd.identity());
    }

    // --- Boundary: wind direction ---

    @Test
    void ensureBoundaryWindDirection0IsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 0.0, 10.0, DATE, START, END));
    }

    @Test
    void ensureBoundaryWindDirection360IsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 360.0, 10.0, DATE, START, END));
    }

    @Test
    void ensureWindDirectionBelow0IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, -1.0, 10.0, DATE, START, END));
    }

    @Test
    void ensureWindDirectionAbove360IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 361.0, 10.0, DATE, START, END));
    }

    // --- Boundary: wind speed ---

    @Test
    void ensureZeroWindSpeedIsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 0.0, DATE, START, END));
    }

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, -0.1, DATE, START, END));
    }

    // --- Boundary: temperature ---

    @Test
    void ensureLatitudeBoundsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", -90.0, -9.0, 40.0, -7.0, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", 90.0, -9.0, 40.0, -7.0, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureLatitudeOutOfBoundsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherData("PT-NORTH", -91.0, -9.0, 40.0, -7.0, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
        assertThrows(IllegalArgumentException.class, () -> new WeatherData("PT-NORTH", 91.0, -9.0, 40.0, -7.0, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    // --- Boundary: pressure ---

    @Test
    void ensurePositivePressureIsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureZeroPressureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, -1.0, 0.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureNegativePressureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, -1.0, 0.0, 90.0, 5.0, DATE, START, END));
    }

    // --- Null fields ---

    @Test
    void ensureNullAirControlAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(null, 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureNullDateTimeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 5.0, null, START, END));
    }

    @Test
    void ensureNullWindDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, null, 40.225, -7.9501, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureNullWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, null, DATE, START, END));
    }

    @Test
    void ensureNullTemperatureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, null, -7.9501, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureNullPressureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 5.0, DATE, null, END));
    }

    // --- Empty string fields ---

    @Test
    void ensureEmptyAirControlAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    @Test
    void ensureBlankAirControlAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("   ", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 5.0, DATE, START, END));
    }

    // --- All getters ---

    @Test
    void ensureAllGettersReturnExpectedValues() {
        final WeatherData wd = validWeatherData();
        assertNotNull(wd.identity());
        assertEquals("PT-NORTH", wd.acaCode());
        assertEquals(DATE, wd.date());
        assertEquals(180.0, wd.direction());
        assertEquals(15.0, wd.windSpeed());
        assertEquals(43.840454, wd.latitude1());
        assertEquals(-9.795711, wd.longitude1());
    }

    // --- sameAs ---

    @Test
    void ensureWeatherDataIsSameAsItself() {
        final WeatherData wd = validWeatherData();
        assertTrue(wd.sameAs(wd));
    }

    @Test
    void ensureDifferentWeatherDataAreNotSameAs() {
        final WeatherData wd1 = new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 180.0, 15.0, DATE, START, END);
        final WeatherData wd2 = new WeatherData("PT-SOUTH", 40.225001, -9.795711, 37.052988, -7.9501, 0.0, 1000.0, 90.0, 10.0, DATE, START, END);
        assertFalse(wd1.sameAs(wd2));
    }

    // --- Equality ---

    @Test
    void ensureWeatherDataIsEqualToItself() {
        final WeatherData wd = validWeatherData();
        assertEquals(wd, wd);
        assertEquals(wd.hashCode(), wd.hashCode());
    }

    @Test
    void ensureWeatherDataIsNotEqualToNull() {
        assertNotEquals(validWeatherData(), null);
    }

    @Test
    void ensureTwoDifferentWeatherDataAreNotEqual() {
        final WeatherData wd1 = new WeatherData("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 180.0, 15.0, DATE, START, END);
        final WeatherData wd2 = new WeatherData("PT-SOUTH", 40.225001, -9.795711, 37.052988, -7.9501, 0.0, 1000.0, 90.0, 10.0, DATE, START, END);
        assertNotEquals(wd1, wd2);
    }
}
