package simulation.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SimulationFlightTest {

    private static final Instant DEP = Instant.parse("2026-06-22T05:30:00Z");

    @Test
    void ensureCreatedWithExpectedState() {
        final SimulationFlight f = new SimulationFlight("TAP0622A", DEP, true, false, null);
        assertAll(
                () -> assertEquals("TAP0622A", f.callsign()),
                () -> assertEquals(DEP, f.departureDate()),
                () -> assertTrue(f.completed()),
                () -> assertFalse(f.hadViolation()),
                () -> assertTrue(f.passedValidation()));
    }

    @Test
    void failedWhenIncompleteOrViolation() {
        assertFalse(new SimulationFlight("A", DEP, false, false, null).passedValidation());
        assertFalse(new SimulationFlight("B", DEP, true, true, null).passedValidation());
    }

    @Test
    void ensureNullCallsignRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationFlight(null, DEP, true, false, null));
    }
}
