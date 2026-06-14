package simulation.application;

import org.junit.jupiter.api.Test;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flight.domain.Flight;
import simulation.domain.Simulation;
import simulation.domain.SimulationStatus;
import simulation.repository.SimulationRepository;
import shared.domain.SimulationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RunAndRecordSimulationControllerTest {

    private static final String JSON = """
        { "meta": { "wallClockStart": "2026-06-22T05:30:00" },
          "flightExecutionStatuses": [ { "callsign": "TAP0622A", "completed": true, "hadViolation": false } ],
          "violationEvents": [] }
        """;

    static class StubSimulationRepository
            extends InMemoryDomainRepository<Simulation, SimulationId>
            implements SimulationRepository {
        void clear() { data().clear(); }
        @Override public Optional<Simulation> findActive() { return Optional.empty(); }
        @Override public List<Simulation> findByStatus(final SimulationStatus s) { return List.of(); }
        @Override public List<Simulation> findByTrackedFlight(final Flight f) { return List.of(); }
        @Override public List<Simulation> findByStartDateTimeBetween(final Instant from, final Instant to) {
            final List<Simulation> result = new ArrayList<>();
            match(s -> !s.startDateTime().isBefore(from) && s.startDateTime().isBefore(to)).forEach(result::add);
            return result;
        }
    }

    private final StubSimulationRepository repo = new StubSimulationRepository();

    @org.junit.jupiter.api.BeforeEach
    void setUp() { repo.clear(); }

    @Test
    void runsRecordsAndReturnsSimulation() {
        final RecordSimulationResultService recorder =
                new RecordSimulationResultService(repo, cs -> null);
        final RunAndRecordSimulationController controller =
                new RunAndRecordSimulationController(() -> JSON, new SimulationResultParser(), recorder);
        final Simulation sim = controller.runAndRecordNoAuth();
        assertEquals(1, sim.simulationFlights().size());
    }
}
