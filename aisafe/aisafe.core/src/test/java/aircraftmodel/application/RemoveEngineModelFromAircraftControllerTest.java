package aircraftmodel.application;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import airtransportcompany.domain.AirTransportCompany;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import enginemodel.repositories.EngineModelRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AircraftId;
import shared.domain.AircraftModelId;
import shared.domain.AirTransportCompanyId;
import shared.domain.EngineModelId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RemoveEngineModelFromAircraftControllerTest {

    private static final String OPERATOR_EMAIL = "rem.eng.operator@test.com";
    private static final String PILOT_EMAIL = "rem.eng.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AircraftModelMaker AM_MAKER = new AircraftModelMaker("Boeing-Rem", "USA");
    private static final EngineModelMaker EM_MAKER = new EngineModelMaker("CFM-Rem", "USA");
    private static final FuelProperties FUEL = new FuelProperties(FuelType.JET_A, 0.8, 43.2);

    private static final AirTransportCompany COMPANY =
            new AirTransportCompany("Rem Airlines", "RE", "REM");
    private static final CabinConfiguration CABIN = new CabinConfiguration(150, 10, 20);

    private final StubAircraftModelRepository modelRepo = new StubAircraftModelRepository();
    private final StubEngineModelRepository engineRepo = new StubEngineModelRepository();
    private final StubAircraftRepository aircraftRepo = new StubAircraftRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {}
        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void setup() {
        modelRepo.clear();
        engineRepo.clear();
        aircraftRepo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private RemoveEngineModelFromAircraftController controller() {
        return new RemoveEngineModelFromAircraftController(modelRepo, engineRepo, aircraftRepo);
    }

    private AircraftModel modelWithTwoEngines() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-Rem", 2, AM_MAKER);
        final EngineModel engine1 = new EngineModel("CFM56-Rem1", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        final EngineModel engine2 = new EngineModel("CFM56-Rem2", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        model.addSupportedEngineModel(engine1);
        model.addSupportedEngineModel(engine2);
        modelRepo.save(model);
        engineRepo.save(engine1);
        engineRepo.save(engine2);
        return model;
    }

    // --- listAircraftModelsWithMultipleEngines ---

    @Test
    void ensureModelsWithMultipleEnginesAreReturned() {
        final AircraftModel model = modelWithTwoEngines();

        final List<AircraftModel> result = controller().listAircraftModelsWithMultipleEngines();
        assertEquals(1, result.size());
    }

    @Test
    void ensureModelWithSingleEngineIsExcluded() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-Rem2", 2, AM_MAKER);
        final EngineModel engine1 = new EngineModel("CFM56-Rem3", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        model.addSupportedEngineModel(engine1);
        modelRepo.save(model);

        final List<AircraftModel> result = controller().listAircraftModelsWithMultipleEngines();
        assertTrue(result.isEmpty());
    }

    // --- listRemovableEngineModels ---

    @Test
    void ensureRemovableEngineModelsAreListed() {
        final AircraftModel model = modelWithTwoEngines();

        final List<EngineModel> removable = controller().listRemovableEngineModels(model);
        assertEquals(2, removable.size());
    }

    // --- removeEngineModel ---

    @Test
    void ensureEngineModelCanBeRemoved() {
        final AircraftModel model = modelWithTwoEngines();
        final EngineModel engineToRemove = model.supportedEngineModels().iterator().next();
        engineRepo.save(engineToRemove);

        final AircraftModel result = controller().removeEngineModel(model, engineToRemove.identity());
        assertEquals(1, result.supportedEngineModels().size());
    }

    @Test
    void ensureRemoveFailsWhenAircraftExistsForModel() {
        final AircraftModel model = modelWithTwoEngines();
        // Create an aircraft using this model
        final EngineModel eng = model.supportedEngineModels().iterator().next();
        final Aircraft aircraft = new Aircraft("CS-R1", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, model, eng, CABIN, COMPANY);
        aircraftRepo.save(aircraft);
        aircraftRepo.markModelExists(model.identity());

        final EngineModel engineToRemove = model.supportedEngineModels().iterator().next();

        assertThrows(IllegalStateException.class, () ->
                controller().removeEngineModel(model, engineToRemove.identity()));
    }

    @Test
    void ensureRemoveFailsWhenEngineModelNotFound() {
        final AircraftModel model = modelWithTwoEngines();

        assertThrows(IllegalArgumentException.class, () ->
                controller().removeEngineModel(model, EngineModelId.newId()));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotListModels() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () -> controller().listAircraftModelsWithMultipleEngines());
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {}
    }

    static class StubAircraftModelRepository
            extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
            implements AircraftModelRepository {

        void clear() { data().clear(); }

        @Override public List<AircraftModel> findByType(final AircraftType t) { return List.of(); }
        @Override public List<AircraftModel> findByMakerName(final String n) { return List.of(); }
        @Override public List<AircraftModel> findByModelName(final String n) { return List.of(); }
        @Override public List<AircraftModel> findByModelNameAndMaker(final String m, final String mk) { return List.of(); }

        @Override
        public List<AircraftModel> findAllAircraftModels() {
            final List<AircraftModel> result = new ArrayList<>();
            findAll().forEach(result::add);
            return result;
        }
    }

    static class StubEngineModelRepository
            extends InMemoryDomainRepository<EngineModel, EngineModelId>
            implements EngineModelRepository {

        void clear() { data().clear(); }

        @Override
        public Optional<EngineModel> findByName(final String name) {
            return matchOne(e -> e.name().equals(name));
        }

        @Override
        public Optional<EngineModel> findByNameAndMakerName(final String name, final String makerName) {
            return matchOne(e -> e.name().equals(name) && e.maker().name().equals(makerName));
        }

        @Override
        public List<EngineModel> findByType(final EngineType type) {
            final List<EngineModel> result = new ArrayList<>();
            match(e -> e.type() == type).forEach(result::add);
            return result;
        }
    }

    static class StubAircraftRepository
            extends InMemoryDomainRepository<Aircraft, AircraftId>
            implements AircraftRepository {

        private final java.util.Set<AircraftModelId> modelIds = new java.util.HashSet<>();

        void clear() { data().clear(); modelIds.clear(); }

        void markModelExists(final AircraftModelId id) { modelIds.add(id); }

        @Override public List<Aircraft> findByCabinConfiguration(final CabinConfiguration c) { return List.of(); }
        @Override public List<Aircraft> findByOperationStatus(final String s) { return List.of(); }
        @Override public List<Aircraft> findByRegistrationCountry(final String c) { return List.of(); }
        @Override public Optional<Aircraft> findByRegistrationNumber(final String n) { return Optional.empty(); }
        @Override public List<Aircraft> findByAirTransportCompanyId(final AirTransportCompanyId id) { return List.of(); }

        @Override
        public boolean existsByAircraftModel(final AircraftModelId id) {
            return modelIds.contains(id);
        }
    }
}
