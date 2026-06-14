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

class RecordSimulationResultServiceTest {

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

    @org.junit.jupiter.api.BeforeEach
    void setUp() { repo.clear(); }

    @Test
    void persistsSimulationWithFlightsAndViolations() {
        final SimulationResultDTO dto = new SimulationResultDTO(
                Instant.parse("2026-06-22T05:30:00Z"), Instant.parse("2026-06-22T10:00:00Z"),
                List.of(new SimulationResultDTO.FlightOutcome("TAP0622A", true, false),
                        new SimulationResultDTO.FlightOutcome("TAP0629X", false, true)),
                List.of(new SimulationResultDTO.ViolationOutcome(
                        Instant.parse("2026-06-29T09:10:00Z"), simulation.domain.ViolationType.COLLISION, List.of("TAP0629X"))));

        // area resolver that returns null for every callsign (no flight-plan dependency in the test)
        final Simulation saved = new RecordSimulationResultService(repo, callsign -> null).record(dto);

        assertEquals(2, saved.simulationFlights().size());
        assertEquals(1, saved.safetyViolations().size());
        assertEquals(1, repo.findByStartDateTimeBetween(
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z")).size());
    }
}
