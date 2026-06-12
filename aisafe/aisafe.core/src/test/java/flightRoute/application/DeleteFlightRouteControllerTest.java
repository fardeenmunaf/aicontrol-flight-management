package flightRoute.application;

import aircontrolarea.domain.AirControlArea;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.domain.FlightType;
import flightPlan.repository.FlightPlanRepository;
import shared.domain.AirportId;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.FlightPlanId;
import shared.domain.FlightRouteId;
import shared.domain.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeleteFlightRouteControllerTest {

    private static final String ATCC_EMAIL = "delete.route.atcc@test.com";
    private static final String PILOT_EMAIL = "delete.route.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AirControlArea AREA = new AirControlArea("PT-DEL", 100.0, "Portugal");
    private static final Airport LISBON = new Airport(
            "Lisbon Airport", "Lisbon", "Portugal", "LIS", "LPPT", 114.0,
            new AirportCoordinates(38.77, -9.13), AREA);
    private static final Airport PORTO = new Airport(
            "Porto Airport", "Porto", "Portugal", "OPO", "LPPR", 69.0,
            new AirportCoordinates(41.24, -8.68), AREA);
    private static AirTransportCompany company;

    private final StubFlightRouteRepository routeRepo = new StubFlightRouteRepository();
    private final StubFlightPlanRepository planRepo = new StubFlightPlanRepository();
    private final StubCollaboratorRepository collaboratorRepo = new StubCollaboratorRepository();
    private final StubCompanyRepository companyRepo = new StubCompanyRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {}
        company = new AirTransportCompany("TAP Delete Test", "TD", "TDL");
        safeRegister(ATCC_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void setup() {
        routeRepo.clear();
        planRepo.clear();
        collaboratorRepo.clear();
        companyRepo.clear();

        companyRepo.save(company);

        final UserId userId = UserId.valueOf(ATCC_EMAIL);
        final CustomerCollaborator collaborator = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, company.identity(), null, "Collaborator");
        collaboratorRepo.save(collaborator);

        AuthzRegistry.authenticationService().authenticate(ATCC_EMAIL, PASSWORD);
    }

    private DeleteFlightRouteController controller() {
        return new DeleteFlightRouteController(routeRepo, planRepo, collaboratorRepo, companyRepo);
    }

    private FlightRoute savedRoute() {
        final FlightRoute route = new FlightRoute(
                FlightRouteId.newId(), new RouteDesignation("TD01"),
                LocalDate.of(2025, 1, 1), LISBON, PORTO, company);
        return routeRepo.save(route);
    }

    @Test
    void ensureRouteIsDeactivated() {
        final FlightRoute route = savedRoute();
        final LocalDate deactivation = LocalDate.of(2027, 6, 1);

        controller().deleteFlightRoute(route.identity().toString(), deactivation);

        final FlightRoute updated = routeRepo.ofIdentity(route.identity()).orElseThrow();
        assertEquals(deactivation, updated.activeUntil());
    }

    @Test
    void ensureRouteNotFoundThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().deleteFlightRoute(FlightRouteId.newId().toString(), LocalDate.now()));
    }

    @Test
    void ensureRouteOfOtherCompanyIsRejected() {
        final AirTransportCompany other = new AirTransportCompany("Other Co", "OC", "OCO");
        companyRepo.save(other);
        final FlightRoute otherRoute = new FlightRoute(
                FlightRouteId.newId(), new RouteDesignation("OC99"),
                LocalDate.of(2025, 1, 1), LISBON, PORTO, other);
        routeRepo.save(otherRoute);

        assertThrows(IllegalStateException.class,
                () -> controller().deleteFlightRoute(otherRoute.identity().toString(), LocalDate.now().plusDays(1)));
    }

    @Test
    void ensurePlannedFlightsAfterDateBlockDeactivation() {
        final FlightRoute route = savedRoute();
        final LocalDate deactivation = LocalDate.of(2027, 1, 1);

        final FlightPlan plan = planOnRoute(route.designation().value(),
                Instant.parse("2027-06-01T10:00:00Z"));
        planRepo.save(plan);

        assertThrows(IllegalStateException.class,
                () -> controller().deleteFlightRoute(route.identity().toString(), deactivation));
    }

    @Test
    void ensureNoPlannedFlightsAfterDateAllowsDeactivation() {
        final FlightRoute route = savedRoute();
        final LocalDate deactivation = LocalDate.of(2027, 1, 1);

        final FlightPlan plan = planOnRoute(route.designation().value(),
                Instant.parse("2026-12-01T10:00:00Z"));
        planRepo.save(plan);

        assertDoesNotThrow(() -> controller().deleteFlightRoute(route.identity().toString(), deactivation));
    }

    private static FlightPlan planOnRoute(final String routeReference, final Instant departure) {
        final FlightLeg leg = new FlightLeg(1, departure, departure.plusSeconds(5400), 4200.0,
                AirportId.valueOf("apt-OPO"), AirportId.valueOf("apt-MAD"));
        return new FlightPlan(FlightPlanId.newId(), "TP1234", FlightType.REGULAR,
                "AC-REG", PILOT_EMAIL, routeReference, "OPO", "MAD", List.of(leg));
    }

    @Test
    void ensureUnauthorizedUserCannotDelete() {
        final FlightRoute route = savedRoute();
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class,
                () -> controller().deleteFlightRoute(route.identity().toString(), LocalDate.now().plusDays(1)));
    }

    @Test
    void ensureCollaboratorNotFoundThrows() {
        final FlightRoute route = savedRoute();
        collaboratorRepo.clear();
        assertThrows(IllegalStateException.class,
                () -> controller().deleteFlightRoute(route.identity().toString(), LocalDate.now().plusDays(1)));
    }

    @Test
    void ensureCompanyNotFoundThrows() {
        final FlightRoute route = savedRoute();
        companyRepo.clear();
        assertThrows(IllegalStateException.class,
                () -> controller().deleteFlightRoute(route.identity().toString(), LocalDate.now().plusDays(1)));
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {}
    }

    // --- Stubs ---

    static class StubFlightRouteRepository
            extends InMemoryDomainRepository<FlightRoute, FlightRouteId>
            implements FlightRouteRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<FlightRoute> findByDesignation(final RouteDesignation designation) {
            return matchOne(r -> r.designation().equals(designation));
        }

        @Override
        public Iterable<FlightRoute> findActiveOn(final LocalDate date) {
            return match(r -> r.isActiveOn(date));
        }

        @Override
        public Iterable<FlightRoute> findByCompanyId(final AirTransportCompanyId companyId) {
            return match(r -> r.company().identity().equals(companyId));
        }
    }

    static class StubFlightPlanRepository
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(fp -> fp.identity().equals(id));
        }

        @Override
        public Iterable<FlightPlan> findByStatus(final FlightPlanStatus status) {
            return match(fp -> fp.status() == status);
        }

        @Override
        public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String routeId, final Instant departure) {
            return match(fp -> routeId.equals(fp.routeReference())
                    && fp.departureTime() != null
                    && fp.departureTime().isAfter(departure));
        }

        @Override
        public Iterable<FlightPlan> findByPilotRef(final String pilotRef) {
            return match(fp -> pilotRef.equals(fp.pilotRef()));
        }
        @Override public Iterable<FlightPlan> findByFlightDesignator(final String flightDesignator) {
            return match(fp -> flightDesignator.equals(fp.flightDesignator()));
        }
    }

    static class StubCollaboratorRepository
            extends InMemoryDomainRepository<CustomerCollaborator, CustomerCollaboratorId>
            implements CustomerCollaboratorRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<CustomerCollaborator> findByUserId(final UserId id) {
            return matchOne(c -> c.userId().equals(id));
        }

        @Override
        public Iterable<CustomerCollaborator> findActiveByAirTransportCompanyId(final AirTransportCompanyId id) {
            final List<CustomerCollaborator> result = new ArrayList<>();
            match(c -> c.isActive() && id.equals(c.airTransportCompanyId())).forEach(result::add);
            return result;
        }

        @Override
        public Iterable<CustomerCollaborator> findActiveByAirControlAreaId(final AirControlAreaId id) {
            return List.of();
        }
    }

    static class StubCompanyRepository
            extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
            implements AirTransportCompanyRepository {

        void clear() { data().clear(); }

        @Override public List<AirTransportCompany> findByName(final String n) { return List.of(); }
        @Override public List<AirTransportCompany> findByIataCode(final String c) { return List.of(); }
        @Override public List<AirTransportCompany> findByIcaoCode(final String c) { return List.of(); }
    }
}
