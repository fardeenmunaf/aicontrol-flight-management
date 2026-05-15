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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AircraftModelId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ViewAircraftModelControllerTest {

    private static final String OPERATOR_EMAIL = "view.aircraft.model.operator@test.com";
    private static final String PILOT_EMAIL = "view.aircraft.model.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private final StubAircraftModelRepository repo = new StubAircraftModelRepository();

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

    private ViewAircraftModelController controller() {
        return new ViewAircraftModelController(repo);
    }

    @Test
    void ensureEmptyRepositoryReturnsEmptyList() {
        final List<AircraftModelDTO> result = controller().listAircraftModels();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureAllModelsAreReturned() {
        repo.save(new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-V1", 2, new AircraftModelMaker("Boeing", "USA")));
        repo.save(new AircraftModel(AircraftType.CARGO,
                60000.0, 130000.0, 110000.0, 40000.0,
                13000.0, 900.0, 180.0, 60.0, 0.03, 1.1,
                "B747F-V1", 4, new AircraftModelMaker("Boeing", "USA")));

        final List<AircraftModelDTO> result = controller().listAircraftModels();
        assertEquals(2, result.size());
    }

    @Test
    void ensureModelDTOHasExpectedFields() {
        repo.save(new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-DTO", 2, new AircraftModelMaker("Boeing", "USA")));

        final List<AircraftModelDTO> result = controller().listAircraftModels();
        assertEquals(1, result.size());
        final AircraftModelDTO dto = result.get(0);
        assertEquals("B737-DTO", dto.modelName);
        assertEquals("Boeing", dto.makerName);
        assertEquals("USA", dto.makerCountry);
    }

    @Test
    void ensureUnauthorizedUserCannotListModels() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(Exception.class, () -> controller().listAircraftModels());
    }

    @Test
    void ensureAircraftModelDTOToStringContainsModelName() {
        repo.save(new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-TS", 2, new AircraftModelMaker("Boeing", "USA")));

        final List<AircraftModelDTO> result = controller().listAircraftModels();
        assertEquals(1, result.size());
        final String str = result.get(0).toString();
        assertTrue(str.contains("B737-TS"));
        assertTrue(str.contains("Boeing"));
    }

    @Test
    void ensureModelWithEnginesListsEngineNames() {
        final AircraftModel model = new AircraftModel(AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737-ENG", 2, new AircraftModelMaker("Boeing", "USA"));
        final enginemodel.domain.EngineModel engine = new enginemodel.domain.EngineModel(
                "CFM56-View", enginemodel.domain.EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new enginemodel.domain.EngineModelMaker("CFM", "USA"),
                new enginemodel.domain.FuelProperties(enginemodel.domain.FuelType.JET_A, 0.8, 43.2));
        model.addSupportedEngineModel(engine);
        repo.save(model);

        final List<AircraftModelDTO> result = controller().listAircraftModels();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).engineModelNames.size());
        assertEquals("CFM56-View", result.get(0).engineModelNames.get(0));
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
}
