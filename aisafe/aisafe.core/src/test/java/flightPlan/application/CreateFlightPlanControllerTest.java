package flightPlan.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import airport.domain.Airport;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.repository.FlightPlanRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotCompanyAssignment;
import pilot.domain.PilotStatus;
import pilot.repository.PilotCompanyAssignmentRepository;
import pilot.repository.PilotRepository;
import shared.domain.AircraftId;
import shared.domain.AirportId;
import shared.domain.AirTransportCompanyId;
import shared.domain.FlightPlanId;
import shared.domain.FlightRouteId;
import shared.domain.PilotId;
import shared.domain.UserId;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateFlightPlanControllerTest {

    private static final String PILOT_EMAIL = "alice@tapportugal.com";
    private static final String PASSWORD = "Password123!";
    private static final String ROUTE_DESIGNATION = "TP123";
    private static final String AIRCRAFT_REG = "CS-TUA";
    private static final Instant FUTURE = Instant.now().plus(30, ChronoUnit.DAYS);
    private static final double FUEL = 15000.0;

    // Company TP
    private static final AirTransportCompany TP_COMPANY = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");

    // Company FR (different)
    private static final AirTransportCompany FR_COMPANY = new AirTransportCompany("Ryanair", "FR", "RYR");

    private final StubFlightPlanRepo planRepo = new StubFlightPlanRepo();
    private final StubFlightRouteRepo routeRepo = new StubFlightRouteRepo();
    private final StubAircraftRepo aircraftRepo = new StubAircraftRepo();
    private final StubPilotRepo pilotRepo = new StubPilotRepo();
    private final StubAssignmentRepo assignmentRepo = new StubAssignmentRepo();
    private final StubCompanyRepo companyRepo = new StubCompanyRepo();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) {
        }
        try {
            AuthzRegistry.userService().registerNewUser(PILOT_EMAIL, PASSWORD, "Alice", "Pilot", PILOT_EMAIL,
                    Set.of(AISafeRoles.PILOT));
        } catch (final Exception ignored) {
        }
    }

    @BeforeEach
    void setup() {
        planRepo.clear();
        routeRepo.clear();
        aircraftRepo.clear();
        pilotRepo.clear();
        assignmentRepo.clear();

        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);

        // Default happy-path state
        pilotRepo.add(new Pilot(UserId.valueOf(PILOT_EMAIL), PilotStatus.ACTIVE, Set.of()));
        assignmentRepo.add(new PilotCompanyAssignment(UserId.valueOf(PILOT_EMAIL), TP_COMPANY.identity()));
        routeRepo.add(activeRoute(ROUTE_DESIGNATION, TP_COMPANY));
        aircraftRepo.add(operationalAircraft(AIRCRAFT_REG, TP_COMPANY));
        companyRepo.setTp(TP_COMPANY);
    }

    private CreateFlightPlanController controller() {
        return new CreateFlightPlanController(planRepo, routeRepo, aircraftRepo, pilotRepo, assignmentRepo, companyRepo);
    }

    // -------------------------------------------------------------------------
    // TC01 – successful creation
    // -------------------------------------------------------------------------

    @Test
    void ensureFlightPlanIsCreatedSuccessfully() {
        final FlightPlan plan = controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null);
        assertNotNull(plan);
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
        final String json = plan.flightPlanJson();
        assertTrue(json.contains(AIRCRAFT_REG));
        assertTrue(json.contains(PILOT_EMAIL));
    }

    // TC05 – status is DRAFT on creation
    @Test
    void ensureStatusIsDraft() {
        final FlightPlan plan = controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null);
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    // -------------------------------------------------------------------------
    // TC02 – inactive route
    // -------------------------------------------------------------------------

    @Test
    void ensureInactiveRouteIsRejected() {
        routeRepo.clear();
        routeRepo.add(inactiveRoute(ROUTE_DESIGNATION, TP_COMPANY));
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    @Test
    void ensureUnknownRouteIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan("TP999", AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // TC03 / TC04 – pilot's company does not match route's company
    // -------------------------------------------------------------------------

    @Test
    void ensurePilotFromDifferentCompanyIsRejected() {
        // Pilot is assigned to FR, but route belongs to TP
        assignmentRepo.clear();
        assignmentRepo.add(new PilotCompanyAssignment(UserId.valueOf(PILOT_EMAIL), FR_COMPANY.identity()));
        companyRepo.setFr(FR_COMPANY);
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // TC05 – aircraft from different company
    // -------------------------------------------------------------------------

    @Test
    void ensureAircraftFromDifferentCompanyIsRejected() {
        aircraftRepo.clear();
        aircraftRepo.add(operationalAircraft(AIRCRAFT_REG, FR_COMPANY));
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // TC06 – decommissioned aircraft
    // -------------------------------------------------------------------------

    @Test
    void ensureDecommissionedAircraftIsRejected() {
        aircraftRepo.clear();
        aircraftRepo.add(decommissionedAircraft(AIRCRAFT_REG, TP_COMPANY));
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // TC07 – past departure
    // -------------------------------------------------------------------------

    @Test
    void ensurePastDepartureDateIsRejected() {
        final Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, past, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // TC08 – zero / negative fuel
    // -------------------------------------------------------------------------

    @Test
    void ensureZeroFuelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, 0.0, null));
    }

    @Test
    void ensureNegativeFuelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, -500.0, null));
    }

    // -------------------------------------------------------------------------
    // TC09 – missing mandatory fields
    // -------------------------------------------------------------------------

    @Test
    void ensureNullRouteIsRejected() {
        assertThrows(Exception.class,
                () -> controller().createFlightPlan(null, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    @Test
    void ensureNullAircraftIsRejected() {
        assertThrows(Exception.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, null, FUTURE, FUEL, null));
    }

    @Test
    void ensureNullDepartureIsRejected() {
        assertThrows(Exception.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, null, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // TC11 – inactive pilot
    // -------------------------------------------------------------------------

    @Test
    void ensureInactivePilotIsRejected() {
        pilotRepo.clear();
        pilotRepo.add(new Pilot(UserId.valueOf(PILOT_EMAIL), PilotStatus.INACTIVE, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // Pilot with no company assignment — must provide company IATA
    // -------------------------------------------------------------------------

    @Test
    void ensurePilotWithoutAssignmentCanUseIataHint() {
        assignmentRepo.clear(); // no assignment
        final FlightPlan plan = controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, "TP");
        assertNotNull(plan);
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensurePilotWithoutAssignmentAndNoHintIsRejected() {
        assignmentRepo.clear();
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightPlan(ROUTE_DESIGNATION, AIRCRAFT_REG, FUTURE, FUEL, null));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static FlightRoute activeRoute(final String designation, final AirTransportCompany company) {
        return new FlightRoute(
                FlightRouteId.newId(),
                new RouteDesignation(designation),
                LocalDate.now().minusDays(1),
                new DummyAirport(),
                new DummyAirport(),
                company);
    }

    private static FlightRoute inactiveRoute(final String designation, final AirTransportCompany company) {
        final FlightRoute route = activeRoute(designation, company);
        route.deactivateFrom(LocalDate.now().minusDays(1));
        return route;
    }

    private static Aircraft operationalAircraft(final String reg, final AirTransportCompany company) {
        return new DummyAircraft(reg, AircraftOperationStatus.IN_SERVICE, company);
    }

    private static Aircraft decommissionedAircraft(final String reg, final AirTransportCompany company) {
        return new DummyAircraft(reg, AircraftOperationStatus.DECOMISSIONED, company);
    }

    // -------------------------------------------------------------------------
    // Stub repositories
    // -------------------------------------------------------------------------

    static class StubFlightPlanRepo extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {
        void clear() { data().clear(); }
        @Override public Optional<FlightPlan> findByFlightPlanId(FlightPlanId id) { return matchOne(p -> p.identity().equals(id)); }
        @Override public Iterable<FlightPlan> findByStatus(FlightPlanStatus s) { return List.of(); }
        @Override public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(String routeId, Instant d) { return List.of(); }
    }

    static class StubFlightRouteRepo extends InMemoryDomainRepository<FlightRoute, FlightRouteId>
            implements FlightRouteRepository {
        void clear() { data().clear(); }
        void add(FlightRoute r) { save(r); }
        @Override public Optional<FlightRoute> findByDesignation(RouteDesignation d) {
            return matchOne(r -> r.designation().value().equals(d.value()));
        }
        @Override public Iterable<FlightRoute> findActiveOn(java.time.LocalDate date) { return List.of(); }
        @Override public Iterable<FlightRoute> findByCompanyId(shared.domain.AirTransportCompanyId id) { return List.of(); }
    }

    static class StubAircraftRepo extends InMemoryDomainRepository<Aircraft, AircraftId>
            implements AircraftRepository {
        void clear() { data().clear(); }
        void add(Aircraft a) { save(a); }
        @Override public Optional<Aircraft> findByRegistrationNumber(String reg) {
            return matchOne(a -> a.registrationNumber().equals(reg));
        }
        @Override public List<Aircraft> findByCabinConfiguration(CabinConfiguration c) { return List.of(); }
        @Override public List<Aircraft> findByOperationStatus(String s) { return List.of(); }
        @Override public List<Aircraft> findByRegistrationCountry(String c) { return List.of(); }
        @Override public List<Aircraft> findByAirTransportCompanyId(AirTransportCompanyId id) { return List.of(); }
        @Override public boolean existsByAircraftModel(shared.domain.AircraftModelId id) { return false; }
    }

    static class StubPilotRepo extends InMemoryDomainRepository<Pilot, PilotId>
            implements PilotRepository {
        void clear() { data().clear(); }
        void add(Pilot p) { save(p); }
        @Override public List<Pilot> findByStatus(PilotStatus s) { return List.of(); }
        @Override public List<Pilot> findCertifiedFor(aircraftmodel.domain.AircraftModel m) { return List.of(); }
        @Override public Optional<Pilot> findByUserId(UserId userId) {
            return matchOne(p -> p.userId().equals(userId));
        }
    }

    static class StubAssignmentRepo extends InMemoryDomainRepository<PilotCompanyAssignment, UserId>
            implements PilotCompanyAssignmentRepository {
        void clear() { data().clear(); }
        void add(PilotCompanyAssignment a) { save(a); }
        @Override public Optional<PilotCompanyAssignment> findByPilotUserId(UserId id) {
            return matchOne(a -> a.pilotUserId().equals(id));
        }
        @Override public List<PilotCompanyAssignment> findByCompanyId(AirTransportCompanyId id) { return List.of(); }
    }

    static class StubCompanyRepo extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
            implements AirTransportCompanyRepository {
        void setTp(AirTransportCompany c) { save(c); }
        void setFr(AirTransportCompany c) { save(c); }
        @Override public List<AirTransportCompany> findByName(String n) { return List.of(); }
        @Override public List<AirTransportCompany> findByIataCode(String iata) {
            return matchAll(c -> c.iataCode().equalsIgnoreCase(iata));
        }
        @Override public List<AirTransportCompany> findByIcaoCode(String icao) { return List.of(); }

        private List<AirTransportCompany> matchAll(java.util.function.Predicate<AirTransportCompany> pred) {
            final List<AirTransportCompany> result = new java.util.ArrayList<>();
            findAll().forEach(c -> { if (pred.test(c)) result.add(c); });
            return result;
        }
    }

    // -------------------------------------------------------------------------
    // Dummy domain objects
    // -------------------------------------------------------------------------

    static class DummyAirport extends Airport {
        private final AirportId id = AirportId.newId();
        DummyAirport() { super(); }
        @Override public AirportId identity() { return id; }
    }

    static class DummyAircraft extends Aircraft {
        private final AircraftId id = AircraftId.newId();
        private final String reg;
        private final AircraftOperationStatus status;
        private final AirTransportCompany company;

        DummyAircraft(String reg, AircraftOperationStatus status, AirTransportCompany company) {
            super();
            this.reg = reg;
            this.status = status;
            this.company = company;
        }

        @Override public AircraftId identity() { return id; }
        @Override public String registrationNumber() { return reg; }
        @Override public AircraftOperationStatus operationStatus() { return status; }
        @Override public AirTransportCompany airTransportCompany() { return company; }
    }
}
