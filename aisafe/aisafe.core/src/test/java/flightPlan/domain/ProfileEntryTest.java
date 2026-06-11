package flightPlan.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileEntryTest {

    @Test
    void ensureClimbEntryIsCreated() {
        final ProfileEntry e = ProfileEntry.climb(1000.0, "m", 210.0, "kts");
        assertAll(
                () -> assertEquals(ProfilePhase.CLIMB, e.phase()),
                () -> assertEquals(1000.0, e.altitude()),
                () -> assertEquals(210.0, e.speed()),
                () -> assertNull(e.rate()));
    }

    @Test
    void ensureCruiseEntryHasNoAltitudeOrRate() {
        final ProfileEntry e = ProfileEntry.cruise(460.0, "kts");
        assertAll(
                () -> assertEquals(ProfilePhase.CRUISE, e.phase()),
                () -> assertNull(e.altitude()),
                () -> assertEquals(460.0, e.speed()),
                () -> assertNull(e.rate()));
    }

    @Test
    void ensureDescendEntryHasNegativeRate() {
        final ProfileEntry e = ProfileEntry.descend(9000.0, "m", 300.0, "kts", -10.0, "m/s");
        assertAll(
                () -> assertEquals(ProfilePhase.DESCEND, e.phase()),
                () -> assertEquals(9000.0, e.altitude()),
                () -> assertEquals(-10.0, e.rate()));
    }

    @Test
    void ensureNonPositiveSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ProfileEntry.cruise(0.0, "kts"));
    }

    @Test
    void ensureDescendRateMustBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> ProfileEntry.descend(9000.0, "m", 300.0, "kts", 5.0, "m/s"));
    }
}
