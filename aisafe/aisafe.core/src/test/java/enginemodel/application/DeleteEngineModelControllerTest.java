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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeleteEngineModelControllerTest {

    private static final String OPERATOR_EMAIL = "del.eng.operator@test.com";
    private static final String PILOT_EMAIL = "del.eng.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final EngineModelMaker MAKER = new EngineModelMaker("RR", "UK");
    private static final FuelProperties FUEL = new FuelProperties(FuelType.JET_A, 0.8, 43.2);

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

    private DeleteEngineModelController controller() {
        return new DeleteEngineModelController(repo);
    }

    @Test
    void ensureActiveEngineModelsAreListed() {
        repo.save(new EngineModel("Trent-XWB", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));

        final Iterable<EngineModel> result = controller().activeEngineModels();
        assertTrue(result.iterator().hasNext());
    }

    @Test
    void ensureEngineModelCanBeDeleted() {
        final EngineModel engine = new EngineModel("DEL-ENG", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL);
        repo.save(engine);

        controller().deleteEngineModel(engine);

        // Verify deleted
        final Iterator<EngineModel> it = repo.findAll().iterator();
        assertFalse(it.hasNext());
    }

    @Test
    void ensureUnauthorizedUserCannotListEngineModels() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () -> controller().activeEngineModels());
    }

    @Test
    void ensureUnauthorizedUserCannotDeleteEngineModel() {
        final EngineModel engine = new EngineModel("PROT-ENG", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL);
        repo.save(engine);

        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () -> controller().deleteEngineModel(engine));
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
