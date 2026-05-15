package enginemodel.application;

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
import shared.domain.EngineModelId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterEngineModelControllerTest {

    private static final String OPERATOR_EMAIL = "eng.operator@test.com";
    private static final String PILOT_EMAIL = "eng.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubEngineModelRepository repo = new StubEngineModelRepository();

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
    void authenticate() {
        repo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private RegisterEngineModelController controller() {
        return new RegisterEngineModelController(repo);
    }

    // --- Happy path ---

    @Test
    void ensureEngineModelIsRegistered() {
        final EngineModel engine = controller().registerEngineModel(
                "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                "CFM", "USA", FuelType.JET_A, 0.8, 43.2);
        assertNotNull(engine);
        assertEquals("CFM56", engine.name());
        assertEquals(EngineType.TURBOFAN, engine.type());
    }

    // --- Duplicate rejection ---

    @Test
    void ensureDuplicateEngineModelIsRejected() {
        controller().registerEngineModel(
                "DUP-ENGINE", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                "CFM", "USA", FuelType.JET_A, 0.8, 43.2);
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "DUP-ENGINE", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", FuelType.JET_A, 0.8, 43.2));
    }

    // --- Invalid arguments ---

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        null, EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", FuelType.JET_A, 0.8, 43.2));
    }

    @Test
    void ensureNullTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "CFM56", null, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", FuelType.JET_A, 0.8, 43.2));
    }

    @Test
    void ensureZeroPowerIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "CFM56", EngineType.TURBOFAN, 0.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", FuelType.JET_A, 0.8, 43.2));
    }

    @Test
    void ensureBlankMakerNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "", "USA", FuelType.JET_A, 0.8, 43.2));
    }

    @Test
    void ensureBlankMakerCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "", FuelType.JET_A, 0.8, 43.2));
    }

    @Test
    void ensureNullFuelTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", null, 0.8, 43.2));
    }

    @Test
    void ensureZeroFuelDensityIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                controller().registerEngineModel(
                        "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", FuelType.JET_A, 0.0, 43.2));
    }

    // --- Authorization ---

    @Test
    void ensureUnauthorizedUserCannotRegisterEngine() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () ->
                controller().registerEngineModel(
                        "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                        "CFM", "USA", FuelType.JET_A, 0.8, 43.2));
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {}
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
