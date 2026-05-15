package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import airtransportcompany.domain.AirTransportCompany;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AircraftId;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ListAircraftControllerTest {

    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56-LA", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
            new EngineModelMaker("CFM", "France"),
            new FuelProperties(FuelType.JET_A, 0.80, 43.0));

    static { MODEL.addSupportedEngineModel(ENGINE); }

    private static final AirTransportCompany COMPANY =
            new AirTransportCompany("Test Airlines", "TA", "TAL");

    private static final CabinConfiguration CABIN = new CabinConfiguration(150, 10, 20);

    private final StubAircraftRepository repo = new StubAircraftRepository();

    @BeforeEach
    void reset() { repo.clear(); }

    private ListAircraftController controller() {
        return new ListAircraftController(repo);
    }

    @Test
    void ensureEmptyRepositoryReturnsNoAircraft() {
        final Iterable<Aircraft> result = controller().listAll();
        assertFalse(result.iterator().hasNext());
    }

    @Test
    void ensureAllAircraftAreReturned() {
        repo.save(new Aircraft("CS-A", "Portugal", AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY));
        repo.save(new Aircraft("CS-B", "Spain", AircraftOperationStatus.IN_SERVICE, 3, MODEL, ENGINE, CABIN, COMPANY));

        int count = 0;
        for (final Aircraft a : controller().listAll()) { count++; }
        assertEquals(2, count);
    }

    @Test
    void ensureSingleAircraftIsReturned() {
        repo.save(new Aircraft("CS-SINGLE", "Portugal", AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY));
        final Iterable<Aircraft> result = controller().listAll();
        assertTrue(result.iterator().hasNext());
    }

    static class StubAircraftRepository
            extends InMemoryDomainRepository<Aircraft, AircraftId>
            implements AircraftRepository {

        void clear() { data().clear(); }

        @Override public List<Aircraft> findByCabinConfiguration(final CabinConfiguration c) { return List.of(); }
        @Override public List<Aircraft> findByOperationStatus(final String s) { return List.of(); }
        @Override public List<Aircraft> findByRegistrationCountry(final String c) { return List.of(); }
        @Override public Optional<Aircraft> findByRegistrationNumber(final String n) { return Optional.empty(); }
        @Override public List<Aircraft> findByAirTransportCompanyId(final shared.domain.AirTransportCompanyId id) { return List.of(); }
        @Override public boolean existsByAircraftModel(final shared.domain.AircraftModelId id) { return false; }
    }
}
