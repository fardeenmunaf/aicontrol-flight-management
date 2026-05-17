package simulationReport.domain;

import org.junit.jupiter.api.Test;
import shared.domain.SimulationId;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SimulationReportTest {

    private static final SimulationId SIM_ID = SimulationId.newId();
    private static final Instant NOW = Instant.now();

    private SimulationReport validReport() {
        return new SimulationReport(10, "OK", NOW, SIM_ID);
    }

    // --- Construction ---

    @Test
    void ensureSimulationReportWithValidDataIsCreated() {
        final SimulationReport r = validReport();
        assertEquals(10, r.totalFlights());
        assertEquals("OK", r.validationResult());
        assertEquals(NOW, r.generatedAt());
        assertEquals(SIM_ID, r.simulationId());
        assertNotNull(r.identity());
    }

    // --- Boundary: totalFlights ---

    @Test
    void ensureZeroTotalFlightsIsAccepted() {
        assertDoesNotThrow(() -> new SimulationReport(0, "OK", NOW, SIM_ID));
    }

    @Test
    void ensureNegativeTotalFlightsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport(-1, "OK", NOW, SIM_ID));
    }

    @Test
    void ensureLargeTotalFlightsIsAccepted() {
        assertDoesNotThrow(() -> new SimulationReport(Integer.MAX_VALUE, "OK", NOW, SIM_ID));
    }

    // --- Null fields ---

    @Test
    void ensureNullTotalFlightsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport(null, "OK", NOW, SIM_ID));
    }

    @Test
    void ensureNullValidationResultIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport(10, null, NOW, SIM_ID));
    }

    @Test
    void ensureNullGeneratedAtIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport(10, "OK", null, SIM_ID));
    }

    @Test
    void ensureNullSimulationIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationReport(10, "OK", NOW, null));
    }

    // --- All getters ---

    @Test
    void ensureAllGettersReturnExpectedValues() {
        final SimulationReport r = validReport();
        assertNotNull(r.identity());
        assertEquals(10, r.totalFlights());
        assertEquals("OK", r.validationResult());
        assertEquals(NOW, r.generatedAt());
        assertEquals(SIM_ID, r.simulationId());
    }

    // --- sameAs ---

    @Test
    void ensureSimulationReportIsSameAsItself() {
        final SimulationReport r = validReport();
        assertTrue(r.sameAs(r));
    }

    @Test
    void ensureDifferentSimulationReportsAreNotSameAs() {
        final SimulationReport r1 = new SimulationReport(5, "OK", NOW, SIM_ID);
        final SimulationReport r2 = new SimulationReport(20, "FAILED", NOW, SimulationId.newId());
        assertFalse(r1.sameAs(r2));
    }

    // --- Equality ---

    @Test
    void ensureSimulationReportIsEqualToItself() {
        final SimulationReport r = validReport();
        assertEquals(r, r);
        assertEquals(r.hashCode(), r.hashCode());
    }

    @Test
    void ensureSimulationReportIsNotEqualToNull() {
        assertNotEquals(validReport(), null);
    }

    @Test
    void ensureTwoDifferentSimulationReportsAreNotEqual() {
        final SimulationReport r1 = new SimulationReport(5, "OK", NOW, SIM_ID);
        final SimulationReport r2 = new SimulationReport(20, "FAILED", NOW, SimulationId.newId());
        assertNotEquals(r1, r2);
    }
}
