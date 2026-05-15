package weatherData.application;

import infrastructure.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weatherData.dto.WeatherDataDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ListWeatherDataController and RegisterWeatherDataController.
 * Uses the InMemoryRepositoryFactory via PersistenceContext.
 */
class WeatherDataControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void configureInMemoryFactory() throws Exception {
        // Reset and configure PersistenceContext to use InMemoryRepositoryFactory via reflection
        System.setProperty("aisafe.persistence.factory", "inmemory.InMemoryRepositoryFactory");
        final java.lang.reflect.Method reset = PersistenceContext.class.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
    }

    // --- RegisterWeatherDataController ---

    @Test
    void ensureValidWeatherDataCanBeRegistered() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, 90.0, 15.0, 20.0, 1013.25);
        assertTrue(ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureBlankAreaIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("", NOW, 90.0, 15.0, 20.0, 1013.25);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, 90.0, -1.0, 20.0, 1013.25);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureInvalidWindDirectionIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, 400.0, 15.0, 20.0, 1013.25);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureNegativeWindDirectionIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, -1.0, 15.0, 20.0, 1013.25);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureNonPositivePressureIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, 90.0, 15.0, 20.0, 0.0);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureTemperatureTooLowIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, 90.0, 15.0, -101.0, 1013.25);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureTemperatureTooHighIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", NOW, 90.0, 15.0, 101.0, 1013.25);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    // --- ListWeatherDataController ---

    @Test
    void ensureListAllReturnsNonNullIterable() {
        final ListWeatherDataController ctrl = new ListWeatherDataController();
        final Iterable<WeatherDataDTO> result = ctrl.listAll();
        assertNotNull(result);
    }

    @Test
    void ensureListAllReturnsRegisteredData() {
        final RegisterWeatherDataController registerCtrl = new RegisterWeatherDataController();
        registerCtrl.registerWeatherData(new WeatherDataDTO("PT-SOUTH", NOW, 180.0, 5.0, 15.0, 1010.0));

        final ListWeatherDataController listCtrl = new ListWeatherDataController();
        final Iterable<WeatherDataDTO> result = listCtrl.listAll();
        assertNotNull(result);
        // After registration, we should have at least one entry
        assertTrue(result.iterator().hasNext());
    }
}
