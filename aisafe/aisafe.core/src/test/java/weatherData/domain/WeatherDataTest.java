package weatherData.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataTest {

    private static final LocalDateTime DT = LocalDateTime.of(2025, 6, 15, 12, 0);

    private WeatherData validWeatherData() {
        return new WeatherData("PT-NORTH", DT, 180.0, 15.0, 22.5, 1013.25);
    }

    // --- Construction ---

    @Test
    void ensureWeatherDataWithValidDataIsCreated() {
        final WeatherData wd = validWeatherData();
        assertEquals("PT-NORTH", wd.airControlArea());
        assertEquals(DT, wd.dateTime());
        assertEquals(180.0, wd.windDirection());
        assertEquals(15.0, wd.windSpeed());
        assertEquals(22.5, wd.temperature());
        assertEquals(1013.25, wd.pressure());
        assertNotNull(wd.identity());
    }

    // --- Boundary: wind direction ---

    @Test
    void ensureBoundaryWindDirection0IsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", DT, 0.0, 10.0, 20.0, 1010.0));
    }

    @Test
    void ensureBoundaryWindDirection360IsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", DT, 360.0, 10.0, 20.0, 1010.0));
    }

    @Test
    void ensureWindDirectionBelow0IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, -1.0, 10.0, 20.0, 1010.0));
    }

    @Test
    void ensureWindDirectionAbove360IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, 361.0, 10.0, 20.0, 1010.0));
    }

    // --- Boundary: wind speed ---

    @Test
    void ensureZeroWindSpeedIsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", DT, 90.0, 0.0, 20.0, 1010.0));
    }

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, -0.1, 20.0, 1010.0));
    }

    // --- Boundary: temperature ---

    @Test
    void ensureBoundaryTemperatureMinus100IsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, -100.0, 1010.0));
    }

    @Test
    void ensureBoundaryTemperature100IsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, 100.0, 1010.0));
    }

    @Test
    void ensureTemperatureBelowMinus100IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, -100.1, 1010.0));
    }

    @Test
    void ensureTemperatureAbove100IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, 100.1, 1010.0));
    }

    // --- Boundary: pressure ---

    @Test
    void ensurePositivePressureIsAccepted() {
        assertDoesNotThrow(() -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, 20.0, 0.1));
    }

    @Test
    void ensureZeroPressureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, 20.0, 0.0));
    }

    @Test
    void ensureNegativePressureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, 20.0, -1.0));
    }

    // --- Null fields ---

    @Test
    void ensureNullAirControlAreaIsRejected() {
        assertThrows(Exception.class,
                () -> new WeatherData(null, DT, 90.0, 5.0, 20.0, 1010.0));
    }

    @Test
    void ensureNullDateTimeIsRejected() {
        assertThrows(Exception.class,
                () -> new WeatherData("PT-NORTH", null, 90.0, 5.0, 20.0, 1010.0));
    }

    @Test
    void ensureNullWindDirectionIsRejected() {
        assertThrows(Exception.class,
                () -> new WeatherData("PT-NORTH", DT, null, 5.0, 20.0, 1010.0));
    }

    @Test
    void ensureNullWindSpeedIsRejected() {
        assertThrows(Exception.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, null, 20.0, 1010.0));
    }

    @Test
    void ensureNullTemperatureIsRejected() {
        assertThrows(Exception.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, null, 1010.0));
    }

    @Test
    void ensureNullPressureIsRejected() {
        assertThrows(Exception.class,
                () -> new WeatherData("PT-NORTH", DT, 90.0, 5.0, 20.0, null));
    }

    // --- Empty string fields ---

    @Test
    void ensureEmptyAirControlAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("", DT, 90.0, 5.0, 20.0, 1010.0));
    }

    @Test
    void ensureBlankAirControlAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData("   ", DT, 90.0, 5.0, 20.0, 1010.0));
    }

    // --- All getters ---

    @Test
    void ensureAllGettersReturnExpectedValues() {
        final WeatherData wd = validWeatherData();
        assertNotNull(wd.identity());
        assertEquals("PT-NORTH", wd.airControlArea());
        assertEquals(DT, wd.dateTime());
        assertEquals(180.0, wd.windDirection());
        assertEquals(15.0, wd.windSpeed());
        assertEquals(22.5, wd.temperature());
        assertEquals(1013.25, wd.pressure());
    }

    // --- sameAs ---

    @Test
    void ensureWeatherDataIsSameAsItself() {
        final WeatherData wd = validWeatherData();
        assertTrue(wd.sameAs(wd));
    }

    @Test
    void ensureDifferentWeatherDataAreNotSameAs() {
        final WeatherData wd1 = new WeatherData("PT-NORTH", DT, 180.0, 15.0, 22.5, 1013.25);
        final WeatherData wd2 = new WeatherData("PT-SOUTH", DT, 90.0, 10.0, 18.0, 1005.0);
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
        final WeatherData wd1 = new WeatherData("PT-NORTH", DT, 180.0, 15.0, 22.5, 1013.25);
        final WeatherData wd2 = new WeatherData("PT-SOUTH", DT, 90.0, 10.0, 18.0, 1005.0);
        assertNotEquals(wd1, wd2);
    }
}
