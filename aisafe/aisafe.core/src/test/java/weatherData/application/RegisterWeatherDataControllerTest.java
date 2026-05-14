package weatherData.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import weatherData.dto.WeatherDataDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RegisterWeatherDataControllerTest {

    @BeforeAll
    static void setup() {
        // Ensure PersistenceContext loads the test in-memory factory
        System.setProperty("aisafe.persistence.factory", "inmemory.InMemoryRepositoryFactory");
    }

    @Test
    void ensureWeatherDataWithMaxTemperatureIsAccepted() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                180.0,
                15.0,
                100.0,
                1013.25);

        assertTrue(ctrl.registerWeatherData(dto));

        final Iterable<WeatherDataDTO> all = new ListWeatherDataController().listAll();
        assertNotNull(all);
        assertTrue(all.iterator().hasNext());
        final WeatherDataDTO saved = all.iterator().next();
        assertEquals(100.0, saved.temperature());
    }

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                90.0,
                -0.1,
                20.0,
                1010.0);

        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }
}

