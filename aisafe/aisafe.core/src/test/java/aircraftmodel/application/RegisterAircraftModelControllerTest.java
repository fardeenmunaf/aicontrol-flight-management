package aircraftmodel.application;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.dto.AircraftModelDTO;
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

class RegisterAircraftModelControllerTest {

    private static final String OPERATOR_EMAIL = "reg.acmodel.operator@test.com";
    private static final String PILOT_EMAIL = "reg.acmodel.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56-RM", EngineType.TURBOFAN,
            100000.0, 0.35, 130000.0, 90000.0, 0.06,
            new EngineModelMaker("CFM", "USA"),
            new FuelProperties(FuelType.JET_A, 0.8, 43.2));

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
        engineRepo.save(ENGINE);
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private RegisterAircraftModelController controller() {
        return new RegisterAircraftModelController(modelRepo, engineRepo);
    }

    private AircraftModelDTO validDto() {
        return new AircraftModelDTO(
                "B737-RM", "Boeing", "USA",
                AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                2, List.of("CFM56-RM"));
    }

    // --- Happy path ---

    @Test
    void ensureAircraftModelIsRegistered() {
        final AircraftModel model = controller().registerAircraftModel(validDto());
        assertNotNull(model);
        assertEquals("B737-RM", model.modelName());
        assertEquals("Boeing", model.maker().name());
    }

    // --- Duplicate rejection ---

    @Test
    void ensureDuplicateModelIsRejected() {
        controller().registerAircraftModel(validDto());
        assertThrows(IllegalArgumentException.class, () -> controller().registerAircraftModel(validDto()));
    }

    // --- Null DTO ---

    @Test
    void ensureNullDtoIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller().registerAircraftModel(null));
    }

    // --- Negative engines ---

    @Test
    void ensureNegativeNumberOfEnginesIsRejected() {
        final AircraftModelDTO dto = new AircraftModelDTO(
                "B737-NEG", "Boeing", "USA",
                AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                -1, List.of());
        assertThrows(IllegalArgumentException.class, () -> controller().registerAircraftModel(dto));
    }

    // --- Zero engines with engine models listed ---

    @Test
    void ensureZeroEnginesWithEngineModelsIsRejected() {
        final AircraftModelDTO dto = new AircraftModelDTO(
                "B737-ZERO", "Boeing", "USA",
                AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                0, List.of("CFM56-RM"));
        assertThrows(IllegalArgumentException.class, () -> controller().registerAircraftModel(dto));
    }

    // --- Non-existent engine model ---

    @Test
    void ensureNonExistentEngineModelIsRejected() {
        final AircraftModelDTO dto = new AircraftModelDTO(
                "B737-NOENG", "Boeing", "USA",
                AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                2, List.of("NonExistentEngine"));
        assertThrows(IllegalArgumentException.class, () -> controller().registerAircraftModel(dto));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotRegisterModel() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () -> controller().registerAircraftModel(validDto()));
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
