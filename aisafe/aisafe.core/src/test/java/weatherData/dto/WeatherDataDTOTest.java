package weatherData.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataDTOTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void ensureWeatherDataDTOIsCreatedWithExpectedState() {
        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH", NOW, 90.0, 15.0, 20.0, 1013.25);
        assertAll(
                () -> assertEquals("PT-NORTH", dto.airControlArea()),
                () -> assertEquals(NOW, dto.dateTime()),
                () -> assertEquals(90.0, dto.windDirection()),
                () -> assertEquals(15.0, dto.windSpeed()),
                () -> assertEquals(20.0, dto.temperature()),
                () -> assertEquals(1013.25, dto.pressure())
        );
    }

    @Test
    void ensureNullValuesAreAccepted() {
        final WeatherDataDTO dto = new WeatherDataDTO(null, null, null, null, null, null);
        assertNull(dto.airControlArea());
        assertNull(dto.dateTime());
        assertNull(dto.windDirection());
        assertNull(dto.windSpeed());
        assertNull(dto.temperature());
        assertNull(dto.pressure());
    }

    @Test
    void ensureAllAccessorsReturnExpectedValues() {
        final WeatherDataDTO dto = new WeatherDataDTO("AREA-1", NOW, 180.0, 5.0, -10.0, 1020.0);
        assertEquals("AREA-1", dto.airControlArea());
        assertEquals(NOW, dto.dateTime());
        assertEquals(180.0, dto.windDirection());
        assertEquals(5.0, dto.windSpeed());
        assertEquals(-10.0, dto.temperature());
        assertEquals(1020.0, dto.pressure());
    }
}
