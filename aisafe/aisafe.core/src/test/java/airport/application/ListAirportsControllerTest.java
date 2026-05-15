package airport.application;

import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;
import aircontrolarea.domain.AirControlArea;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirportId;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ListAirportsControllerTest {

    private final StubAirportRepository repo = new StubAirportRepository();
    private final AirControlArea AREA = new AirControlArea("PT", 1000.0, "41,8;41,9;42,9;42,8");
    private final AirportCoordinates COORDS = new AirportCoordinates(41.24, -8.68);

    @BeforeEach
    void reset() { repo.clear(); }

    private ListAirportsController controller() {
        return new ListAirportsController(repo);
    }

    @Test
    void ensureEmptyRepositoryReturnsNoAirports() {
        assertFalse(controller().listAirports().iterator().hasNext());
    }

    @Test
    void ensureAllAirportsAreReturned() {
        repo.save(new Airport("Porto", "Porto", "Portugal", "OPO", "LPPR", 69.0, COORDS, AREA));
        repo.save(new Airport("Lisboa", "Lisboa", "Portugal", "LIS", "LPPT", 114.0, COORDS, AREA));

        int count = 0;
        for (final Airport a : controller().listAirports()) { count++; }
        assertEquals(2, count);
    }

    @Test
    void ensureSingleAirportIsReturned() {
        repo.save(new Airport("Porto", "Porto", "Portugal", "OPO", "LPPR", 69.0, COORDS, AREA));
        assertTrue(controller().listAirports().iterator().hasNext());
    }

    static class StubAirportRepository
            extends InMemoryDomainRepository<Airport, AirportId>
            implements AirportRepository {

        void clear() { data().clear(); }

        @Override public Optional<Airport> findByIataCode(final String c) { return Optional.empty(); }
        @Override public Optional<Airport> findByIcaoCode(final String c) { return Optional.empty(); }
    }
}
