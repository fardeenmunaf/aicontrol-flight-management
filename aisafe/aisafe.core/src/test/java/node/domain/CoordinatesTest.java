package node.domain;

import org.junit.jupiter.api.Test;

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

    @Test
    void ensureLatitudeLessThanMinusNinetyIsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(-90.001, 0.0));
    }

    @Test
    void ensureLatitudeGreaterThanNinetyIsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(90.001, 0.0));
    }

    @Test
    void ensureLongitudeLessThanMinusOneHundredEightyIsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(0.0, -180.001));
    }

    @Test
    void ensureLongitudeGreaterThanOneHundredEightyIsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(0.0, 180.001));
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
