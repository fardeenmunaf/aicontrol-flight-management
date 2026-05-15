package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flight.domain.Flight;
import flight.domain.FlightStatus;
import flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AircraftId;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.FlightId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DecommissionAircraftControllerTest {

    private static final String ATCC_EMAIL = "decom.atcc@test.com";
    private static final String PILOT_EMAIL = "decom.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737-decom", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56-DC", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
            new EngineModelMaker("CFM", "France"),
            new FuelProperties(FuelType.JET_A, 0.80, 43.0));

    static { MODEL.addSupportedEngineModel(ENGINE); }

    private static AirTransportCompany company;

    private final StubAircraftRepository aircraftRepo = new StubAircraftRepository();
    private final StubCollaboratorRepository collaboratorRepo = new StubCollaboratorRepository();
    private final StubCompanyRepository companyRepo = new StubCompanyRepository();
    private final StubFlightRepository flightRepo = new StubFlightRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {}
        company = new AirTransportCompany("Decom Airlines", "DC", "DCA");
        safeRegister(ATCC_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void setup() {
        aircraftRepo.clear();
        collaboratorRepo.clear();
        companyRepo.clear();
        flightRepo.clear();

        companyRepo.save(company);

        // Register collaborator for ATCC_EMAIL
        final UserId userId = UserId.valueOf(ATCC_EMAIL);
        final CustomerCollaborator collaborator = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, company.identity(), null, "Manager");
        collaboratorRepo.save(collaborator);

        AuthzRegistry.authenticationService().authenticate(ATCC_EMAIL, PASSWORD);
    }

    private DecommissionAircraftController controller() {
        return new DecommissionAircraftController(aircraftRepo, collaboratorRepo, companyRepo, flightRepo);
    }

    @Test
    void ensureActiveAircraftIsDecommissioned() {
        final Aircraft aircraft = new Aircraft("CS-D1", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE,
                new CabinConfiguration(150, 10, 20), company);
        aircraftRepo.save(aircraft);

        controller().decommissionAircraft(aircraft.identity().toString());

        final Aircraft saved = aircraftRepo.ofIdentity(aircraft.identity()).orElseThrow();
        assertEquals(AircraftOperationStatus.DECOMISSIONED, saved.operationStatus());
    }

    @Test
    void ensureNonExistentAircraftIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller().decommissionAircraft(AircraftId.newId().toString()));
    }

    @Test
    void ensureAlreadyDecommissionedAircraftIsRejected() {
        final Aircraft aircraft = new Aircraft("CS-D2", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE,
                new CabinConfiguration(150, 10, 20), company);
        aircraft.decommission();
        aircraftRepo.save(aircraft);

        assertThrows(IllegalStateException.class,
                () -> controller().decommissionAircraft(aircraft.identity().toString()));
    }

    @Test
    void ensureUnauthorizedUserCannotDecommission() {
        final Aircraft aircraft = new Aircraft("CS-D3", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE,
                new CabinConfiguration(150, 10, 20), company);
        aircraftRepo.save(aircraft);

        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class,
                () -> controller().decommissionAircraft(aircraft.identity().toString()));
    }

    @Test
    void ensureDecommissionFailsWhenCollaboratorNotFound() {
        // Remove collaborator to trigger "not a customer collaborator" exception
        collaboratorRepo.clear();

        final Aircraft aircraft = new Aircraft("CS-D4", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE,
                new CabinConfiguration(150, 10, 20), company);
        aircraftRepo.save(aircraft);

        assertThrows(IllegalStateException.class,
                () -> controller().decommissionAircraft(aircraft.identity().toString()));
    }

    @Test
    void ensureDecommissionFailsWhenCompanyNotFound() {
        // Remove company to trigger "Air transport company not found" exception
        companyRepo.clear();

        final Aircraft aircraft = new Aircraft("CS-D5", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE,
                new CabinConfiguration(150, 10, 20), company);
        aircraftRepo.save(aircraft);

        assertThrows(IllegalStateException.class,
                () -> controller().decommissionAircraft(aircraft.identity().toString()));
    }

    @Test
    void ensureDecommissionFailsWhenAircraftBelongsToOtherCompany() {
        final AirTransportCompany otherCompany = new AirTransportCompany("Other Airlines", "OT", "OTA");
        companyRepo.save(otherCompany);

        final Aircraft aircraft = new Aircraft("CS-D6", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE,
                new CabinConfiguration(150, 10, 20), otherCompany);
        aircraftRepo.save(aircraft);

        assertThrows(IllegalStateException.class,
                () -> controller().decommissionAircraft(aircraft.identity().toString()));
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {}
    }

    static class StubAircraftRepository
            extends InMemoryDomainRepository<Aircraft, AircraftId>
            implements AircraftRepository {

        void clear() { data().clear(); }

        @Override public List<Aircraft> findByCabinConfiguration(final CabinConfiguration c) { return List.of(); }
        @Override public List<Aircraft> findByOperationStatus(final String s) { return List.of(); }
        @Override public List<Aircraft> findByRegistrationCountry(final String c) { return List.of(); }
        @Override public Optional<Aircraft> findByRegistrationNumber(final String n) {
            return matchOne(a -> a.registrationCountry().equals(n));
        }
        @Override public List<Aircraft> findByAirTransportCompanyId(final shared.domain.AirTransportCompanyId id) { return List.of(); }
        @Override public boolean existsByAircraftModel(final shared.domain.AircraftModelId id) { return false; }
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

    static class StubFlightRepository
            extends InMemoryDomainRepository<Flight, FlightId>
            implements FlightRepository {

        void clear() { data().clear(); }

        @Override public List<Flight> findActiveFlightsLandingInAirport(final String iataCode) { return List.of(); }
        @Override public List<Flight> findByStatus(final FlightStatus status) { return List.of(); }
        @Override public Optional<Flight> findByFlightDesignator(final String d) { return Optional.empty(); }
        @Override public List<Flight> findByAircraftAndStatus(final AircraftId aircraftId, final FlightStatus status) { return List.of(); }
    }
}
