package simulation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AircraftPositionTest {

    private static final Instant START = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T10:00:00Z");

    private Simulation simulation() {
        return new Simulation(START, END, SimulationStatus.RUNNING);
    }

    private VelocityVector velocity() {
        return new VelocityVector(250.0, 90.0);
    }

    // --- Construction: happy path ---

    @Test
    void ensureAircraftPositionIsCreatedWithExpectedState() {
        final Simulation sim = simulation();
        final VelocityVector vel = velocity();
        final Instant ts = Instant.parse("2026-01-01T09:00:00Z");

        final AircraftPosition pos = new AircraftPosition(sim, ts, 10000.0, vel);
        assertAll(
                () -> assertEquals(sim, pos.simulation()),
                () -> assertEquals(ts, pos.timestamp()),
                () -> assertEquals(10000.0, pos.altitude()),
                () -> assertEquals(vel, pos.velocityVector())
        );
    }

    @Test
    void ensureZeroAltitudeIsAccepted() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 0.0, velocity());
        assertEquals(0.0, pos.altitude());
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullSimulationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftPosition(null, START, 10000.0, velocity()));
    }

    @Test
    void ensureNullTimestampIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftPosition(simulation(), null, 10000.0, velocity()));
    }

    @Test
    void ensureNullAltitudeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftPosition(simulation(), START, null, velocity()));
    }

    @Test
    void ensureNullVelocityVectorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftPosition(simulation(), START, 10000.0, null));
    }

    @Test
    void ensureNegativeAltitudeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftPosition(simulation(), START, -1.0, velocity()));
    }

    // --- equals / hashCode ---

    @Test
    void ensurePositionIsEqualToItself() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 10000.0, velocity());
        assertEquals(pos, pos);
    }

    @Test
    void ensurePositionIsNotEqualToNull() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 10000.0, velocity());
        assertNotEquals(pos, null);
    }

    @Test
    void ensurePositionHashCodeIsConsistent() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 10000.0, velocity());
        assertEquals(pos.hashCode(), pos.hashCode());
    }

    @Test
    void ensurePositionIdentityIsNullBeforePersistence() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 10000.0, velocity());
        assertNull(pos.identity());
    }

    @Test
    void ensurePositionSameAsItself() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 10000.0, velocity());
        assertTrue(pos.sameAs(pos));
    }

    @Test
    void ensurePositionSameAsNullIsFalse() {
        final AircraftPosition pos = new AircraftPosition(simulation(), START, 10000.0, velocity());
        assertFalse(pos.sameAs(null));
    }
}
