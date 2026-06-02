package flightPlan.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.repository.FlightPlanRepository;
import node.domain.Node;
import node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.FlightPlanId;
import shared.domain.NodeId;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import airport.domain.Airport;
import airport.repositories.AirportRepository;
import shared.domain.AircraftModelId;
import shared.domain.AirportId;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import shared.domain.AirTransportCompanyId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImportFlightPlanControllerTest {

    private static final String PILOT_EMAIL = "import.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubFlightPlanRepository planRepo = new StubFlightPlanRepository();
    private final StubNodeRepository nodeRepo = new StubNodeRepository();
    private final StubAircraftModelRepository aircraftRepo = new StubAircraftModelRepository();
    private final StubAirportRepository airportRepo = new StubAirportRepository();
    private final StubAirTransportCompanyRepository companyRepo = new StubAirTransportCompanyRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) {
        }
        try {
            AuthzRegistry.userService().registerNewUser(PILOT_EMAIL, PASSWORD, "P", "P", PILOT_EMAIL,
                    Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) {
        }
    }

    @BeforeEach
    void setup() {
        planRepo.clear();
        nodeRepo.clear();
        aircraftRepo.clear();
        airportRepo.clear();
        companyRepo.clear();
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
    }

    private ImportFlightPlanController controller() {
        return new ImportFlightPlanController(planRepo, nodeRepo, aircraftRepo, airportRepo, companyRepo);
    }

    private LegImportRequest singleLeg() {
        return new LegImportRequest(1,
                "2026-05-05", "08:00", "2026-05-05", "09:30",
                40200.0, "OPO", "MAD",
                List.of(new SegmentImportRequest(41.26, -8.68, 40.49, -3.56)));
    }

    @Test
    void ensureFlightPlanIsImportedSuccessfully() {
        final FlightPlan plan = controller().importFlightPlan("TP1234", "A380", List.of(singleLeg()));
        assertNotNull(plan);
    }

    @Test
    void ensureNullDesignatorIsRejected() {
        assertThrows(NullPointerException.class,
                () -> controller().importFlightPlan(null, "A380", List.of(singleLeg())));
    }

    @Test
    void ensureEmptyLegsListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller().importFlightPlan("TP1234", "A380", List.of()));
    }

    @Test
    void ensureNullLegsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller().importFlightPlan("TP1234", "A380", null));
    }

    @Test
    void ensureDuplicateDesignatorIsRejected() {
        controller().importFlightPlan("TP-DUPE", "A380", List.of(singleLeg()));
        assertThrows(IllegalArgumentException.class,
                () -> controller().importFlightPlan("TP-DUPE", "A380", List.of(singleLeg())));
    }

    @Test
    void ensureMultipleLegsAreImported() {
        final LegImportRequest leg2 = new LegImportRequest(2,
                "2026-05-05", "11:00", "2026-05-05", "12:30",
                38000.0, "MAD", "BCN",
                List.of(new SegmentImportRequest(40.49, -3.56, 41.29, 2.07)));
        final FlightPlan plan = controller().importFlightPlan("TP5678", "A380", List.of(singleLeg(), leg2));
        assertNotNull(plan);
    }

    @Test
    void ensureInvalidCompanyCallsignIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller().importFlightPlan("XX1234", "A380", List.of(singleLeg())));
    }

    static class StubFlightPlanRepository
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {

        void clear() {
            data().clear();
        }

        @Override
        public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(p -> p.identity().equals(id));
        }

        @Override
        public Iterable<FlightPlan> findByStatus(final FlightPlanStatus s) {
            return List.of();
        }

        @Override
        public Iterable<FlightPlan> findByDepartureBetween(final Instant a, final Instant b) {
            return List.of();
        }

        @Override
        public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String routeId, final Instant departure) {
            return List.of();
        }
    }

    static class StubNodeRepository
            extends InMemoryDomainRepository<Node, NodeId>
            implements NodeRepository {

        void clear() {
            data().clear();
        }

        @Override
        public Optional<Node> findByDesignation(final String d) {
            return Optional.empty();
        }
    }

    static class DummyAirport extends Airport {
        private final AirportId id = AirportId.newId();
        DummyAirport() { super(); }
        @Override public AirportId identity() { return id; }
    }

    static class DummyAircraftModel extends AircraftModel {
        private final AircraftModelId id;
        DummyAircraftModel(AircraftModelId id) { super(); this.id = id; }
        @Override public AircraftModelId identity() { return id; }
    }

    static class StubAircraftModelRepository
            extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
            implements AircraftModelRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<AircraftModel> ofIdentity(final AircraftModelId id) {
            return Optional.of(new DummyAircraftModel(id));
        }

        @Override public List<AircraftModel> findByType(AircraftType type) { return List.of(); }
        @Override public List<AircraftModel> findByMakerName(String makerName) { return List.of(); }
        @Override public List<AircraftModel> findByModelName(String modelName) {
            return List.of(new DummyAircraftModel(AircraftModelId.newId()));
        }
        @Override public List<AircraftModel> findByModelNameAndMaker(String modelName, String makerName) { return List.of(); }
        @Override public List<AircraftModel> findAllAircraftModels() { return List.of(); }
    }

    static class StubAirportRepository
            extends InMemoryDomainRepository<Airport, AirportId>
            implements AirportRepository {

        private final java.util.Map<String, Airport> byIata = new java.util.HashMap<>();

        void clear() { data().clear(); byIata.clear(); }

        @Override
        public Optional<Airport> findByIataCode(String code) {
            return Optional.of(byIata.computeIfAbsent(code, k -> new DummyAirport()));
        }

        @Override
        public Optional<Airport> findByIcaoCode(String code) {
            return Optional.empty();
        }
    }

    static class StubAirTransportCompanyRepository
            extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
            implements AirTransportCompanyRepository {

        void clear() { data().clear(); }

        @Override
        public List<AirTransportCompany> findByName(String name) {
            return List.of();
        }

        @Override
        public List<AirTransportCompany> findByIataCode(String iataCode) {
            if ("TP".equals(iataCode)) {
                return List.of(new AirTransportCompany("TAP", "TP", "TAP"));
            }
            return List.of();
        }

        @Override
        public List<AirTransportCompany> findByIcaoCode(String icaoCode) {
            return List.of();
        }
    }
}
