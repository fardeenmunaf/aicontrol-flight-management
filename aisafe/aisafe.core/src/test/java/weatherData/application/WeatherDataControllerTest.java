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

    private static final java.time.LocalDate DATE = java.time.LocalDate.of(2026, 1, 1);
    private static final java.time.LocalTime START = java.time.LocalTime.of(12, 0);
    private static final java.time.LocalTime END = java.time.LocalTime.of(13, 0);

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
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 15.0, DATE, START, END);
        assertTrue(ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureBlankAreaIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 15.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, -1.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureInvalidWindDirectionIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 400.0, 15.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureNegativeWindDirectionIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, 1000.0, -1.0, 15.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureAltitudeSupNegativeIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        // altitudeSup negative should be rejected
        final WeatherDataDTO dto = new WeatherDataDTO("PT-NORTH", 43.840454, -9.795711, 40.225, -7.9501, 0.0, -10.0, 90.0, 15.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dto));
    }

    @Test
    void ensureLatitudeOutOfBoundsIsRejected() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();
        final WeatherDataDTO dtoLowLat = new WeatherDataDTO("PT-NORTH", -100.0, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 15.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dtoLowLat));
        final WeatherDataDTO dtoHighLat = new WeatherDataDTO("PT-NORTH", 100.0, -9.795711, 40.225, -7.9501, 0.0, 1000.0, 90.0, 15.0, DATE, START, END);
        assertThrows(IllegalArgumentException.class, () -> ctrl.registerWeatherData(dtoHighLat));
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
        registerCtrl.registerWeatherData(new WeatherDataDTO("PT-SOUTH", 40.225001, -9.795711, 37.052988, -7.9501, 0.0, 1000.0, 180.0, 5.0, DATE, START, END));

        final ListWeatherDataController listCtrl = new ListWeatherDataController();
        final Iterable<WeatherDataDTO> result = listCtrl.listAll();
        assertNotNull(result);
        // After registration, we should have at least one entry
        assertTrue(result.iterator().hasNext());
    }
}
