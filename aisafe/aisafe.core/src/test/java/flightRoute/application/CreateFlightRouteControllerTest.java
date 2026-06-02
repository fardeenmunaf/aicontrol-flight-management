package flightRoute.application;

import aircontrolarea.domain.AirControlArea;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;
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
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.AirportId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.FlightRouteId;
import shared.domain.UserId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateFlightRouteControllerTest {

    private static final String ATCC_EMAIL = "create.route.atcc@test.com";
    private static final String PILOT_EMAIL = "create.route.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AirControlArea AREA = new AirControlArea("PT", 100.0, "Portugal");
    private static final Airport LISBON = new Airport(
            "Lisbon Airport", "Lisbon", "Portugal", "LIS", "LPPT", 114.0,
            new AirportCoordinates(38.77, -9.13), AREA);
    private static final Airport PORTO = new Airport(
            "Porto Airport", "Porto", "Portugal", "OPO", "LPPR", 69.0,
            new AirportCoordinates(41.24, -8.68), AREA);
    private static AirTransportCompany company;

    private final StubFlightRouteRepository routeRepo = new StubFlightRouteRepository();
    private final StubAirportRepository airportRepo = new StubAirportRepository();
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
        company = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
        safeRegister(ATCC_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void setup() {
        routeRepo.clear();
        airportRepo.clear();
        collaboratorRepo.clear();
        companyRepo.clear();

        companyRepo.save(company);
        airportRepo.save(LISBON);
        airportRepo.save(PORTO);

        final UserId userId = UserId.valueOf(ATCC_EMAIL);
        final CustomerCollaborator collaborator = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, company.identity(), null, "Collaborator");
        collaboratorRepo.save(collaborator);

        AuthzRegistry.authenticationService().authenticate(ATCC_EMAIL, PASSWORD);
    }

    private CreateFlightRouteController controller() {
        return new CreateFlightRouteController(routeRepo, airportRepo, collaboratorRepo, companyRepo);
    }

    @Test
    void ensureValidRouteIsCreated() {
        final FlightRoute route = controller().createFlightRoute("TP123", "LIS", "OPO", LocalDate.now());

        assertNotNull(route);
        assertEquals("TP123", route.designation().value());
        assertEquals(LISBON.iataCode(), route.originAirport().iataCode());
        assertEquals(PORTO.iataCode(), route.destinationAirport().iataCode());
        assertEquals(company.identity(), route.company().identity());
    }

    @Test
    void ensureDuplicateDesignationIsRejected() {
        controller().createFlightRoute("TP123", "LIS", "OPO", LocalDate.now());

        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightRoute("TP123", "LIS", "OPO", LocalDate.now()));
    }

    @Test
    void ensureWrongCompanyPrefixIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightRoute("BA123", "LIS", "OPO", LocalDate.now()));
    }

    @Test
    void ensureSameOriginAndDestinationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightRoute("TP123", "LIS", "LIS", LocalDate.now()));
    }

    @Test
    void ensureNonExistentOriginIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightRoute("TP123", "XXX", "OPO", LocalDate.now()));
    }

    @Test
    void ensureNonExistentDestinationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().createFlightRoute("TP123", "LIS", "XXX", LocalDate.now()));
    }

    @Test
    void ensureUnauthorizedUserCannotCreate() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class,
                () -> controller().createFlightRoute("TP123", "LIS", "OPO", LocalDate.now()));
    }

    @Test
    void ensureCollaboratorNotFoundThrows() {
        collaboratorRepo.clear();
        assertThrows(IllegalStateException.class,
                () -> controller().createFlightRoute("TP123", "LIS", "OPO", LocalDate.now()));
    }

    @Test
    void ensureCompanyNotFoundThrows() {
        companyRepo.clear();
        assertThrows(IllegalStateException.class,
                () -> controller().createFlightRoute("TP123", "LIS", "OPO", LocalDate.now()));
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

    static class StubAirportRepository
            extends InMemoryDomainRepository<Airport, AirportId>
            implements AirportRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<Airport> findByIataCode(final String iataCode) {
            return matchOne(a -> a.iataCode().equalsIgnoreCase(iataCode));
        }

        @Override
        public Optional<Airport> findByIcaoCode(final String icaoCode) {
            return matchOne(a -> a.icaoCode().equalsIgnoreCase(icaoCode));
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
