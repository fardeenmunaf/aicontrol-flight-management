package aircraftmodel.application;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
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
import shared.domain.AircraftModelId;
import shared.domain.EngineModelId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AddEngineModelToAircraftControllerTest {

    private static final String OPERATOR_EMAIL = "add.eng.operator@test.com";
    private static final String PILOT_EMAIL = "add.eng.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final AircraftModelMaker AM_MAKER = new AircraftModelMaker("Boeing", "USA");
    private static final EngineModelMaker EM_MAKER = new EngineModelMaker("CFM", "USA");
    private static final FuelProperties FUEL = new FuelProperties(FuelType.JET_A, 0.8, 43.2);

    private final StubAircraftModelRepository modelRepo = new StubAircraftModelRepository();
    private final StubEngineModelRepository engineRepo = new StubEngineModelRepository();

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
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private AddEngineModelToAircraftController controller() {
        return new AddEngineModelToAircraftController(modelRepo, engineRepo);
    }

    // --- listAircraftModelsWithRemainingSlots ---

    @Test
    void ensureModelsWithRemainingEnginesAreReturned() {
        // model with 2 engines and 0 assigned
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE1", 2, AM_MAKER);
        modelRepo.save(model);

        final List<AircraftModel> result = controller().listAircraftModelsWithRemainingSlots();
        assertEquals(1, result.size());
    }

    @Test
    void ensureFullyPopulatedModelsAreExcluded() {
        // model with 0 engines (no slots)
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE2", 0, AM_MAKER);
        modelRepo.save(model);

        final List<AircraftModel> result = controller().listAircraftModelsWithRemainingSlots();
        assertTrue(result.isEmpty());
    }

    // --- addEngineModelToAircraft ---

    @Test
    void ensureEngineModelIsAdded() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE3", 2, AM_MAKER);
        modelRepo.save(model);

        final EngineModel engine = new EngineModel("CFM56-AE", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        engineRepo.save(engine);

        final AircraftModel result = controller().addEngineModelToAircraft(
                "B737-AE3", "Boeing", "CFM56-AE");
        assertEquals(1, result.supportedEngineModels().size());
    }

    @Test
    void ensureNonExistentAircraftModelIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("NonExistent", "Boeing", "CFM56"));
    }

    @Test
    void ensureNonExistentEngineModelIsRejected() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE4", 2, AM_MAKER);
        modelRepo.save(model);

        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("B737-AE4", "Boeing", "NonExistentEngine"));
    }

    @Test
    void ensureBlankAircraftModelNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("", "Boeing", "CFM56"));
    }

    @Test
    void ensureBlankEngineModelNameIsRejected() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE5", 2, AM_MAKER);
        modelRepo.save(model);

        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("B737-AE5", "Boeing", ""));
    }

    // --- listAssignableEngineModels ---

    @Test
    void ensureAssignableEngineModelsExcludesAlreadyAssigned() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-LA1", 2, AM_MAKER);
        modelRepo.save(model);

        final EngineModel engine1 = new EngineModel("CFM56-LA1", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        final EngineModel engine2 = new EngineModel("CFM56-LA2", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        engineRepo.save(engine1);
        engineRepo.save(engine2);

        // Assign engine1 to model
        model.addSupportedEngineModel(engine1);

        // listAssignableEngineModels should only return engine2
        final List<EngineModel> assignable = controller().listAssignableEngineModels(model);
        assertEquals(1, assignable.size());
        assertEquals("CFM56-LA2", assignable.get(0).name());
    }

    @Test
    void ensureListAssignableEngineModelsReturnsAllWhenNoneAssigned() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-LA2", 2, AM_MAKER);
        modelRepo.save(model);

        final EngineModel engine = new EngineModel("CFM56-LA3", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        engineRepo.save(engine);

        final List<EngineModel> assignable = controller().listAssignableEngineModels(model);
        assertEquals(1, assignable.size());
    }

    @Test
    void ensureFullyAssignedAircraftRejectsAddEngine() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE6", 1, AM_MAKER);
        modelRepo.save(model);

        final EngineModel engine = new EngineModel("CFM56-AE6", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        engineRepo.save(engine);
        model.addSupportedEngineModel(engine);

        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("B737-AE6", "Boeing", "CFM56-AE6"));
    }

    @Test
    void ensureDuplicateEngineModelAssociationIsRejected() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-AE7", 2, AM_MAKER);
        modelRepo.save(model);

        final EngineModel engine = new EngineModel("CFM56-AE7", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06, EM_MAKER, FUEL);
        engineRepo.save(engine);
        model.addSupportedEngineModel(engine);

        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("B737-AE7", "Boeing", "CFM56-AE7"));
    }

    @Test
    void ensureBlankAircraftMakerIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().addEngineModelToAircraft("B737-AE8", "", "CFM56"));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotListModels() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () -> controller().listAircraftModelsWithRemainingSlots());
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

        @Override
        public List<AircraftModel> findByModelNameAndMaker(final String modelName, final String makerName) {
            final List<AircraftModel> result = new ArrayList<>();
            match(m -> m.modelName().equals(modelName) && m.maker().name().equals(makerName)).forEach(result::add);
            return result;
        }

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
}
