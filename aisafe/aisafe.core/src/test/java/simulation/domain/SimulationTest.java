package simulation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {

    private static final Instant START = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T10:00:00Z");

    private Simulation validSimulation() {
        return new Simulation(START, END, SimulationStatus.CREATED);
    }

    // --- Construction: happy path ---

    @Test
    void ensureSimulationIsCreatedWithExpectedState() {
        final Simulation sim = validSimulation();
        assertAll(
                () -> assertEquals(START, sim.startDateTime()),
                () -> assertEquals(END, sim.endDateTime()),
                () -> assertEquals(SimulationStatus.CREATED, sim.status()),
                () -> assertNotNull(sim.identity()),
                () -> assertTrue(sim.trackedFlights().isEmpty()),
                () -> assertTrue(sim.capturedWeatherData().isEmpty())
        );
    }

    @Test
    void ensureStartEqualsEndIsAccepted() {
        assertDoesNotThrow(() -> new Simulation(START, START, SimulationStatus.RUNNING));
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Simulation(START, END, null));
    }

    @Test
    void ensureNullStartDateTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Simulation(null, END, SimulationStatus.CREATED));
    }

    @Test
    void ensureNullEndDateTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Simulation(START, null, SimulationStatus.CREATED));
    }

    @Test
    void ensureEndBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulation(END, START, SimulationStatus.CREATED));
    }

    // --- Different statuses ---

    @Test
    void ensureRunningSimulationIsCreated() {
        final Simulation sim = new Simulation(START, END, SimulationStatus.RUNNING);
        assertEquals(SimulationStatus.RUNNING, sim.status());
    }

    @Test
    void ensureCompletedSimulationIsCreated() {
        final Simulation sim = new Simulation(START, END, SimulationStatus.COMPLETED);
        assertEquals(SimulationStatus.COMPLETED, sim.status());
    }

    // --- equals / hashCode ---

    @Test
    void ensureSimulationIsEqualToItself() {
        final Simulation sim = validSimulation();
        assertEquals(sim, sim);
        assertEquals(sim.hashCode(), sim.hashCode());
    }

    @Test
    void ensureSimulationIsNotEqualToNull() {
        assertNotEquals(validSimulation(), null);
    }

    @Test
    void ensureTwoDifferentSimulationsAreNotEqual() {
        final Simulation s1 = validSimulation();
        final Simulation s2 = validSimulation();
        assertNotEquals(s1, s2);
    }

    @Test
    void ensureSimulationSameAsItself() {
        final Simulation sim = validSimulation();
        assertTrue(sim.sameAs(sim));
    }

    @Test
    void ensureSimulationSameAsNullIsFalse() {
        final Simulation sim = validSimulation();
        assertFalse(sim.sameAs(null));
    }

    @Test
    void recordsFlightsAndViolations() {
        final Simulation sim = new Simulation(
                java.time.Instant.parse("2026-06-22T00:00:00Z"),
                java.time.Instant.parse("2026-06-22T10:00:00Z"),
                SimulationStatus.COMPLETED);
        sim.addFlight(new SimulationFlight("TAP0622A",
                java.time.Instant.parse("2026-06-22T05:30:00Z"), true, false, null));
        sim.addViolation(new SafetyViolation(sim,
                java.time.Instant.parse("2026-06-22T06:00:00Z"), ViolationType.COLLISION, null));
        assertEquals(1, sim.simulationFlights().size());
        assertEquals(1, sim.safetyViolations().size());
    }
}
