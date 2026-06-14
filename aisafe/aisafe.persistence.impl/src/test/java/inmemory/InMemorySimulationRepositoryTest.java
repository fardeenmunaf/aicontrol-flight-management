package inmemory;

import org.junit.jupiter.api.Test;
import simulation.domain.Simulation;
import simulation.domain.SimulationStatus;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InMemorySimulationRepositoryTest {

    private Simulation simAt(final String iso) {
        return new Simulation(Instant.parse(iso), Instant.parse(iso).plusSeconds(3600),
                SimulationStatus.COMPLETED);
    }

    @Test
    void findsSimulationsWithinRange() {
        final InMemorySimulationRepository repo = new InMemorySimulationRepository();
        repo.save(simAt("2026-06-10T08:00:00Z"));
        repo.save(simAt("2026-07-05T08:00:00Z"));
        final List<Simulation> june = repo.findByStartDateTimeBetween(
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));
        assertEquals(1, june.size());
    }
}
