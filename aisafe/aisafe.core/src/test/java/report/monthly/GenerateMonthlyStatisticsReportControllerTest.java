package report.monthly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flight.domain.Flight;
import simulation.domain.Simulation;
import simulation.domain.SimulationStatus;
import simulation.repository.SimulationRepository;
import shared.domain.SimulationId;

import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMonthlyStatisticsReportControllerTest {

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

    private GenerateMonthlyStatisticsReportController controller(final Path dir) {
        return new GenerateMonthlyStatisticsReportController(
                new MonthlyStatisticsCollector(repo), dir);
    }

    @Test
    void rejectsMissingMonthOrYear() {
        final Exception e = assertThrows(IllegalArgumentException.class,
                () -> controller(Path.of("target")).generateNoAuth(null, null));
        assertEquals("Month and year are required to generate the monthly report.", e.getMessage());
    }

    @Test
    void rejectsInvalidMonth() {
        final Exception e = assertThrows(IllegalArgumentException.class,
                () -> controller(Path.of("target")).generateNoAuth(2026, 13));
        assertEquals("The specified month is not a valid calendar month.", e.getMessage());
    }

    @Test
    void writesIdentifiableFile() throws Exception {
        final Path dir = Files.createTempDirectory("reports");
        final Path file = controller(dir).generateNoAuth(2026, 6);
        assertEquals("monthly_statistics_2026_06.pdf", file.getFileName().toString());
        assertTrue(Files.size(file) > 0);
    }
}
