package aircontrolarea.application;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirControlAreaId;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListAirControlAreasControllerTest {

    private static final String OPERATOR_EMAIL = "list.aca.operator@test.com";
    private static final String PILOT_EMAIL = "list.aca.pilot@test.com";
    private static final String PASSWORD = "Password123";

    private static final String VALID_BOUNDARIES = "1.0,1.0;1.0,-1.0;-1.0,-1.0;-1.0,1.0";

    private final StubAirControlAreaRepository repo = new StubAirControlAreaRepository();

    @BeforeAll
    static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {
        }
        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void authenticate() {
        repo.clear();
        AuthzRegistry.authenticationService().authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private ListAirControlAreasController controller() {
        return new ListAirControlAreasController(repo);
    }

    @Test
    void ensureEmptyRepositoryReturnsEmptyList() {
        final List<AirControlArea> result = controller().allAirControlAreas();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureAllAreasAreReturned() {
        repo.save(new AirControlArea("PT-A", 100.0, VALID_BOUNDARIES));
        repo.save(new AirControlArea("PT-B", 200.0, VALID_BOUNDARIES));

        final List<AirControlArea> result = controller().allAirControlAreas();
        assertEquals(2, result.size());
    }

    @Test
    void ensureSingleAreaIsReturned() {
        repo.save(new AirControlArea("PT-SINGLE", 100.0, VALID_BOUNDARIES));

        final List<AirControlArea> result = controller().allAirControlAreas();
        assertEquals(1, result.size());
        assertEquals("PT-SINGLE", result.get(0).areaCode());
    }

    @Test
    void ensureUnauthorizedUserCannotListAreas() {
        AuthzRegistry.authenticationService().authenticate(PILOT_EMAIL, PASSWORD);
        assertThrows(RuntimeException.class, () -> controller().allAirControlAreas());
    }

    private static void safeRegister(final String email, final String password,
            final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {
        }
    }

    static class StubAirControlAreaRepository
            extends InMemoryDomainRepository<AirControlArea, AirControlAreaId>
            implements AirControlAreaRepository {

        void clear() {
            data().clear();
        }

        @Override
        public List<AirControlArea> findByAreaCode(final String areaCode) {
            return List.of();
        }

        @Override
        public List<AirControlArea> findByMinimumRequiredArrivalFuelGreaterThanEqual(final double minFuel) {
            return List.of();
        }

        @Override
        public List<AirControlArea> findByGeographicBoundaries(final String geographicBoundaries) {
            return List.of();
        }
    }
}
