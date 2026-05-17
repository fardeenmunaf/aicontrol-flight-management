package node.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CoordinatesTest {

    // --- Construction: happy path ---

    @Test
    void ensureCoordinatesAreCreatedWithExpectedState() {
        final Coordinates coords = new Coordinates(38.7169, -9.1399);
        assertAll(
                () -> assertEquals(38.7169, coords.latitude()),
                () -> assertEquals(-9.1399, coords.longitude())
        );
    }

    @Test
    void ensureBoundaryValuesAreAccepted() {
        assertDoesNotThrow(() -> new Coordinates(-90.0, -180.0));
        assertDoesNotThrow(() -> new Coordinates(90.0, 180.0));
        assertDoesNotThrow(() -> new Coordinates(0.0, 0.0));
    }

    // --- Construction: invalid arguments ---

    @ParameterizedTest
    @ValueSource(doubles = { -90.001, 90.001, 200.0 })
    void ensureInvalidLatitudesAreRejected(final double lat) {
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(lat, 0.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = { -180.001, 180.001, 360.0 })
    void ensureInvalidLongitudesAreRejected(final double lon) {
        assertThrows(IllegalArgumentException.class, () -> new Coordinates(0.0, lon));
    }

    // --- equals / hashCode ---

    @Test
    void ensureEqualCoordinatesAreEqual() {
        final Coordinates c1 = new Coordinates(38.7169, -9.1399);
        final Coordinates c2 = new Coordinates(38.7169, -9.1399);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void ensureDifferentCoordinatesAreNotEqual() {
        final Coordinates c1 = new Coordinates(38.7169, -9.1399);
        final Coordinates c2 = new Coordinates(40.4165, -3.7026);
        assertNotEquals(c1, c2);
    }

    @Test
    void ensureCoordinatesAreNotEqualToNull() {
        assertNotEquals(new Coordinates(0.0, 0.0), null);
    }

    @Test
    void ensureCoordinatesAreEqualToThemselves() {
        final Coordinates coords = new Coordinates(38.7169, -9.1399);
        assertEquals(coords, coords);
    }

    // --- toString ---

    @Test
    void ensureToStringIsNonEmpty() {
        final Coordinates coords = new Coordinates(38.7169, -9.1399);
        final String str = coords.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final Coordinates coords = new Coordinates() { };
        assertNotNull(coords);
    }
}
