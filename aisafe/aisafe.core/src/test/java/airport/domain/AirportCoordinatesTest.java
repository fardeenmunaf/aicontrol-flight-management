package airport.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AirportCoordinatesTest {

    @Test
    void ensureValidCoordinatesAreAccepted() {
        final AirportCoordinates c = new AirportCoordinates(38.77, -9.13);
        assertEquals(38.77, c.latitude());
        assertEquals(-9.13, c.longitude());
    }

    @ParameterizedTest
    @ValueSource(doubles = { 90.0, -90.0, 0.0, 41.24 })
    void ensureValidLatitudesAreAccepted(final double lat) {
        assertDoesNotThrow(() -> new AirportCoordinates(lat, 0.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = { 180.0, -180.0, 0.0, -8.67 })
    void ensureValidLongitudesAreAccepted(final double lon) {
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, lon));
    }

    @ParameterizedTest
    @ValueSource(doubles = { 90.1, -90.1, 200.0 })
    void ensureInvalidLatitudesAreRejected(final double lat) {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(lat, 0.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = { 180.1, -180.1, 360.0 })
    void ensureInvalidLongitudesAreRejected(final double lon) {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(0.0, lon));
    }

    @Test
    void ensureNullLatitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(null, 0.0));
    }

    @Test
    void ensureNullLongitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(0.0, null));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final AirportCoordinates coords = new AirportCoordinates() { };
        assertNotNull(coords);
    }
}
