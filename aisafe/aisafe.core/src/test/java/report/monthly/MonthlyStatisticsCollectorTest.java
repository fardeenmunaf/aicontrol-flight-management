package report.monthly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flight.domain.Flight;
import simulation.domain.SafetyViolation;
import simulation.domain.Simulation;
import simulation.domain.SimulationFlight;
import simulation.domain.SimulationStatus;
import simulation.repository.SimulationRepository;
import shared.domain.SimulationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyStatisticsCollectorTest {

    /** Minimal in-memory stub for SimulationRepository — no persistence.impl dependency needed. */
    static class StubSimulationRepository
            extends InMemoryDomainRepository<Simulation, SimulationId>
            implements SimulationRepository {

        void clear() { data().clear(); }

        @Override public Optional<Simulation> findActive() { return Optional.empty(); }
        @Override public List<Simulation> findByStatus(final SimulationStatus s) { return List.of(); }
        @Override public List<Simulation> findByTrackedFlight(final Flight f) { return List.of(); }

        @Override
        public List<Simulation> findByStartDateTimeBetween(final Instant from, final Instant to) {
            final List<Simulation> result = new ArrayList<>();
            match(s -> !s.startDateTime().isBefore(from) && s.startDateTime().isBefore(to))
                    .forEach(result::add);
            return result;
        }
    }

    private final StubSimulationRepository repo = new StubSimulationRepository();

    @BeforeEach
    void setUp() { repo.clear(); }

    private Simulation june(final boolean completed, final boolean violation) {
        final Simulation s = new Simulation(Instant.parse("2026-06-22T05:30:00Z"),
                Instant.parse("2026-06-22T10:00:00Z"), SimulationStatus.COMPLETED);
        s.addFlight(new SimulationFlight("F", Instant.parse("2026-06-22T05:30:00Z"),
                completed, violation, null));
        if (violation) s.addViolation(new SafetyViolation(s,
                Instant.parse("2026-06-22T06:00:00Z"), simulation.domain.ViolationType.COLLISION, null));
        return s;
    }

    @Test
    void aggregatesCountsRateAndPassFail() {
        repo.save(june(true, false));
        repo.save(june(false, true));
        final MonthlyStatistics st = new MonthlyStatisticsCollector(repo).collect(2026, 6);
        assertEquals(2, st.simulationCount());
        assertEquals(2, st.flightCount());
        assertEquals(1, st.violationCount());
        assertEquals(0.5, st.violationRate(), 0.001);
        assertEquals(1, st.passedCount());
        assertEquals(1, st.failedCount());
        assertFalse(st.isEmpty());
    }

    @Test
    void emptyMonthYieldsEmptyStats() {
        final MonthlyStatistics st = new MonthlyStatisticsCollector(repo).collect(2025, 1);
        assertTrue(st.isEmpty());
        assertEquals(0, st.flightCount());
    }
}
