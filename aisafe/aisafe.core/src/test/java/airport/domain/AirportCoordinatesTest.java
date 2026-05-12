package airport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirportCoordinatesTest {

    @Test
    void shouldCreateWithValidCoordinates() {
        final AirportCoordinates c = new AirportCoordinates(38.77, -9.13);
        assertEquals(38.77, c.latitude());
        assertEquals(-9.13, c.longitude());
    }

    @Test
    void shouldAcceptBoundaryLatitude() {
        assertDoesNotThrow(() -> new AirportCoordinates(90.0, 0.0));
        assertDoesNotThrow(() -> new AirportCoordinates(-90.0, 0.0));
    }

    @Test
    void shouldAcceptBoundaryLongitude() {
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, 180.0));
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, -180.0));
    }

    @Test
    void shouldThrowWhenLatitudeAbove90() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(90.1, 0.0));
    }

    @Test
    void shouldThrowWhenLatitudeBelow90() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(-90.1, 0.0));
    }

    @Test
    void shouldThrowWhenLongitudeAbove180() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(0.0, 180.1));
    }

    @Test
    void shouldThrowWhenLongitudeBelow180() {
        assertThrows(IllegalArgumentException.class, () -> new AirportCoordinates(0.0, -180.1));
    }

    @Test
    void shouldThrowWhenLatitudeIsNull() {
        assertThrows(Exception.class, () -> new AirportCoordinates(null, 0.0));
    }

    @Test
    void shouldThrowWhenLongitudeIsNull() {
        assertThrows(Exception.class, () -> new AirportCoordinates(0.0, null));
    }
}
