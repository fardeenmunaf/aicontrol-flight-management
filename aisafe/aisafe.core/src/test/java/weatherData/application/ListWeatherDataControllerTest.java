package weatherData.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import weatherData.dto.WeatherDataDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ListWeatherDataControllerTest {

    @BeforeAll
    static void setup() {
        System.setProperty("aisafe.persistence.factory", "inmemory.InMemoryRepositoryFactory");
    }

    @Test
    void ensureListAllReturnsSavedItems() {
        final RegisterWeatherDataController reg = new RegisterWeatherDataController();
        final ListWeatherDataController listCtrl = new ListWeatherDataController();

        final WeatherDataDTO dto = new WeatherDataDTO(
                "PT-NORTH",
                LocalDateTime.now(),
                45.0,
                5.0,
                10.0,
                1000.0);

        assertTrue(reg.registerWeatherData(dto));

        final Iterable<WeatherDataDTO> all = listCtrl.listAll();
        assertNotNull(all);
        assertTrue(all.iterator().hasNext());
        final WeatherDataDTO saved = all.iterator().next();
        assertEquals("PT-NORTH", saved.airControlArea());
    }
}

