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

class ListEngineModelsControllerTest {

    private static final String OPERATOR_EMAIL = "list.eng.operator@test.com";
    private static final String PILOT_EMAIL = "list.eng.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final EngineModelMaker MAKER = new EngineModelMaker("CFM", "USA");
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

    private ListEngineModelsController controller() {
        return new ListEngineModelsController(repo);
    }

    @Test
    void ensureEmptyRepositoryReturnsNoEngineModels() {
        final Iterable<EngineModel> result = controller().listEngineModels();
        assertFalse(result.iterator().hasNext());
    }

    @Test
    void ensureAllEngineModelsAreReturned() {
        repo.save(new EngineModel("E1", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));
        repo.save(new EngineModel("E2", EngineType.TURBOPROP, 50000.0, 0.30, 60000.0, 45000.0, 0.05, MAKER, FUEL));

        final Iterator<EngineModel> it = controller().listEngineModels().iterator();
        int count = 0;
        while (it.hasNext()) { it.next(); count++; }
        assertEquals(2, count);
    }

    @Test
    void ensureUnauthorizedUserCannotListEngineModels() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () -> controller().listEngineModels());
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
