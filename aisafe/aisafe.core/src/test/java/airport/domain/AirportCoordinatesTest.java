package airport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirportCoordinatesTest {

    @Test
    void ensureValidCoordinatesAreAccepted() {
        final AirportCoordinates c = new AirportCoordinates(38.77, -9.13);
        assertEquals(38.77, c.latitude());
        assertEquals(-9.13, c.longitude());
    }

    @Test
    void ensureBoundaryLatitudeMaxIsAccepted() {
        assertDoesNotThrow(() -> new AirportCoordinates(90.0, 0.0));
    }

    @Test
    void ensureBoundaryLatitudeMinIsAccepted() {
        assertDoesNotThrow(() -> new AirportCoordinates(-90.0, 0.0));
    }

    @Test
    void ensureBoundaryLongitudeMaxIsAccepted() {
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, 180.0));
    }

    @Test
    void ensureBoundaryLongitudeMinIsAccepted() {
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, -180.0));
    }

    @Test
    void ensureLatitudeAbove90IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(90.1, 0.0));
    }

    @Test
    void ensureLatitudeBelow90IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(-90.1, 0.0));
    }

    @Test
    void ensureLongitudeAbove180IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(0.0, 180.1));
    }

    @Test
    void ensureLongitudeBelow180IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(0.0, -180.1));
    }

    @Test
    void ensureNullLatitudeIsRejected() {
        assertThrows(Exception.class, () -> new AirportCoordinates(null, 0.0));
    }

    @Test
    void ensureNullLongitudeIsRejected() {
        assertThrows(Exception.class, () -> new AirportCoordinates(0.0, null));
    }
}
