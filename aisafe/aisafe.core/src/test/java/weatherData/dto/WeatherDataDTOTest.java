package weatherData.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataDTOTest {

    private static final java.time.LocalDate DATE = java.time.LocalDate.of(2026, 1, 1);
    private static final java.time.LocalTime START = java.time.LocalTime.of(12, 0);
    private static final java.time.LocalTime END = java.time.LocalTime.of(13, 0);

    @Test
    void ensureWeatherDataDTOIsCreatedWithExpectedState() {
        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 15.0, DATE, START, END);
        assertAll(
                () -> assertEquals("PT-NORTH", dto.acaCode()),
                () -> assertEquals(DATE, dto.date()),
                () -> assertEquals(90.0, dto.direction()),
                () -> assertEquals(15.0, dto.windSpeed()),
                () -> assertEquals(43.840454, dto.latitude1()),
                () -> assertEquals(-9.795711, dto.longitude1()));
    }

    @Test
    void ensureNullValuesAreAccepted() {
        final WeatherDataDTO dto = new WeatherDataDTO(null, null, null, null, null, null, null, null, null, null, null,
                null);
        assertNull(dto.acaCode());
        assertNull(dto.date());
        assertNull(dto.direction());
        assertNull(dto.windSpeed());
    }

    @Test
    void ensureAllAccessorsReturnExpectedValues() {
        final WeatherDataDTO dto = new WeatherDataDTO("AREA-1", 43.84, -9.79, 40.22, -7.95, 0.0, 1000.0, 180.0, 5.0,
                DATE, START, END);
        assertEquals("AREA-1", dto.acaCode());
        assertEquals(DATE, dto.date());
        assertEquals(180.0, dto.direction());
        assertEquals(5.0, dto.windSpeed());
    }
}
