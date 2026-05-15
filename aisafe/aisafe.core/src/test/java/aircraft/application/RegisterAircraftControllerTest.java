package aircraft.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import enginemodel.repositories.EngineModelRepository;
import shared.domain.AirControlAreaId;
import shared.domain.AircraftId;
import shared.domain.AircraftModelId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.EngineModelId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterAircraftControllerTest {

    private static final String ATCC_EMAIL = "reg.aircraft.atcc@test.com";
    private static final String PILOT_EMAIL = "reg.aircraft.pilot@test.com";
    private static final String PASSWORD = "Password123";

    // A model where payload budget = (65000-40000-500)/100 = 245 people max
    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737-RA", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56-RA", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
            new EngineModelMaker("CFM", "France"),
            new FuelProperties(FuelType.JET_A, 0.80, 43.0));

    static { MODEL.addSupportedEngineModel(ENGINE); }

    private static AirTransportCompany company;

    private final StubAircraftRepository aircraftRepo = new StubAircraftRepository();
    private final StubAircraftModelRepository modelRepo = new StubAircraftModelRepository();
    private final StubCollaboratorRepository collaboratorRepo = new StubCollaboratorRepository();
    private final StubCompanyRepository companyRepo = new StubCompanyRepository();
    private final StubEngineModelRepository engineRepo = new StubEngineModelRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {}
        company = new AirTransportCompany("RA Airlines", "RA", "RAL");
        safeRegister(ATCC_EMAIL, PASSWORD, AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void setup() {
        aircraftRepo.clear();
        modelRepo.clear();
        collaboratorRepo.clear();
        companyRepo.clear();

        companyRepo.save(company);
        modelRepo.save(MODEL);
        engineRepo.save(ENGINE);

        final UserId userId = UserId.valueOf(ATCC_EMAIL);
        final CustomerCollaborator collaborator = new CustomerCollaborator(
                userId, CustomerType.AIR_TRANSPORT_COMPANY, company.identity(), null, "Manager");
        collaboratorRepo.save(collaborator);

        AuthzRegistry.authenticationService().authenticate(ATCC_EMAIL, PASSWORD);
    }

    private RegisterAircraftController controller() {
        return new RegisterAircraftController(aircraftRepo, modelRepo, collaboratorRepo, companyRepo, engineRepo);
    }

    // --- Happy path ---

    @Test
    void ensureAircraftIsRegistered() {
        final Aircraft aircraft = controller().registerAircraft(
                "CS-RA1", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2,
                MODEL.identity().toString(),
                ENGINE.identity().toString(),
                100, 20, 10);
        assertNotNull(aircraft);
        assertEquals("Portugal", aircraft.registrationCountry());
        assertEquals(AircraftOperationStatus.IN_SERVICE, aircraft.operationStatus());
    }

    // --- Duplicate registration number ---

    @Test
    void ensureDuplicateRegistrationNumberIsRejected() {
        // Pre-populate the repository with a known registration number
        aircraftRepo.trackByRegNumber("CS-DUP", new Aircraft(
                "CS-DUP", "Portugal", AircraftOperationStatus.IN_SERVICE, 2,
                MODEL, ENGINE, new CabinConfiguration(50, 5, 10), company));
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("CS-DUP", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 50, 10, 5));
    }

    // --- Invalid arguments ---

    @Test
    void ensureBlankRegistrationNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 100, 20, 10));
    }

    @Test
    void ensureBlankCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("CS-X1", "",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 100, 20, 10));
    }

    @Test
    void ensureNullStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("CS-X2", "Portugal",
                        null, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 100, 20, 10));
    }

    @Test
    void ensureNonExistentModelIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("CS-X3", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        AircraftModelId.newId().toString(), ENGINE.identity().toString(), 10, 2, 1));
    }

    @Test
    void ensureNonExistentEngineModelIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("CS-X5", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), EngineModelId.newId().toString(), 10, 2, 1));
    }

    @Test
    void ensureRegisterFailsWhenCollaboratorNotFound() {
        collaboratorRepo.clear();
        assertThrows(IllegalStateException.class, () ->
                controller().registerAircraft("CS-RF1", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 10, 2, 1));
    }

    @Test
    void ensureRegisterFailsWhenCompanyNotFound() {
        companyRepo.clear();
        assertThrows(IllegalStateException.class, () ->
                controller().registerAircraft("CS-RF2", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 10, 2, 1));
    }

    @Test
    void ensureRegisterFailsWhenTotalPeopleExceedsLimit() {
        // payload budget = (65000-40000-500)/100 = 245
        // crew=2, economy=200, first=50, business=50 -> total=302 > 245
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerAircraft("CS-RF3", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 200, 50, 50));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotRegisterAircraft() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () ->
                controller().registerAircraft("CS-X4", "Portugal",
                        AircraftOperationStatus.IN_SERVICE, 2,
                        MODEL.identity().toString(), ENGINE.identity().toString(), 100, 20, 10));
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

        private final java.util.Map<String, Aircraft> byRegNumber = new java.util.HashMap<>();

        void clear() { data().clear(); byRegNumber.clear(); }

        @Override
        public Aircraft save(final Aircraft entity) {
            final Aircraft saved = super.save(entity);
            // Track by registration country as a proxy (since registrationNumber() returns ID)
            // We store mapping from regCountry (not ideal) — instead track via byRegNumber
            return saved;
        }

        @Override public List<Aircraft> findByCabinConfiguration(final CabinConfiguration c) { return List.of(); }
        @Override public List<Aircraft> findByOperationStatus(final String s) { return List.of(); }
        @Override public List<Aircraft> findByRegistrationCountry(final String c) { return List.of(); }

        @Override
        public Optional<Aircraft> findByRegistrationNumber(final String n) {
            // Since Aircraft.registrationNumber() returns the identity, we can't match easily.
            // But for tests: track separately in byRegNumber map
            return Optional.ofNullable(byRegNumber.get(n));
        }

        public void trackByRegNumber(final String regNumber, final Aircraft aircraft) {
            byRegNumber.put(regNumber, aircraft);
        }

        @Override public List<Aircraft> findByAirTransportCompanyId(final shared.domain.AirTransportCompanyId id) { return List.of(); }
        @Override public boolean existsByAircraftModel(final shared.domain.AircraftModelId id) { return false; }
    }

    static class StubAircraftModelRepository
            extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
            implements AircraftModelRepository {

        void clear() { data().clear(); }

        @Override public List<AircraftModel> findByType(final aircraftmodel.domain.AircraftType t) { return List.of(); }
        @Override public List<AircraftModel> findByMakerName(final String n) { return List.of(); }
        @Override public List<AircraftModel> findByModelName(final String n) { return List.of(); }
        @Override public List<AircraftModel> findByModelNameAndMaker(final String m, final String mk) { return List.of(); }
        @Override public List<AircraftModel> findAllAircraftModels() {
            final List<AircraftModel> result = new ArrayList<>();
            findAll().forEach(result::add);
            return result;
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

    static class StubEngineModelRepository
            extends InMemoryDomainRepository<EngineModel, EngineModelId>
            implements EngineModelRepository {

        void clear() { data().clear(); }

        @Override public java.util.Optional<EngineModel> findByName(final String name) { return java.util.Optional.empty(); }
        @Override public java.util.Optional<EngineModel> findByNameAndMakerName(final String name, final String maker) { return java.util.Optional.empty(); }
        @Override public List<EngineModel> findByType(final enginemodel.domain.EngineType type) { return List.of(); }
    }
}
