package flightRoute.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteDesignationTest {

    // --- Construction: happy path ---

    @Test
    void ensureValidDesignationIsAccepted() {
        final RouteDesignation rd = new RouteDesignation("TP123");
        assertEquals("TP123", rd.value());
    }

    @Test
    void ensureSingleDigitDesignationIsAccepted() {
        assertDoesNotThrow(() -> new RouteDesignation("BA1"));
    }

    @Test
    void ensureFourDigitDesignationIsAccepted() {
        assertDoesNotThrow(() -> new RouteDesignation("LH9999"));
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullDesignationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDesignation(null));
    }

    @Test
    void ensureEmptyDesignationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDesignation(""));
    }

    @Test
    void ensureDesignationWithOnlyLettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDesignation("ABCD"));
    }

    @Test
    void ensureDesignationWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDesignation("A123"));
    }

    @Test
    void ensureDesignationWithFiveDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDesignation("TP12345"));
    }

    @Test
    void ensureLowercaseDesignationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDesignation("tp123"));
    }

    // --- equals / hashCode ---

    @Test
    void ensureEqualDesignationsAreEqual() {
        final RouteDesignation rd1 = new RouteDesignation("TP123");
        final RouteDesignation rd2 = new RouteDesignation("TP123");
        assertEquals(rd1, rd2);
        assertEquals(rd1.hashCode(), rd2.hashCode());
    }

    @Test
    void ensureDifferentDesignationsAreNotEqual() {
        final RouteDesignation rd1 = new RouteDesignation("TP123");
        final RouteDesignation rd2 = new RouteDesignation("BA456");
        assertNotEquals(rd1, rd2);
    }

    @Test
    void ensureDesignationIsNotEqualToNull() {
        assertNotEquals(new RouteDesignation("TP123"), null);
    }

    // --- toString ---

    @Test
    void ensureToStringReturnsDesignation() {
        assertEquals("TP123", new RouteDesignation("TP123").toString());
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final RouteDesignation rd = new RouteDesignation() { };
        assertNotNull(rd);
    }
}
