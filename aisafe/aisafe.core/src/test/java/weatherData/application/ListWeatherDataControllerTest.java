package weatherData.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weatherData.dto.WeatherDataDTO;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListWeatherDataControllerTest {

    private static final String OPERATOR_EMAIL = "list.operator@test.com";
    private static final String PILOT_EMAIL = "list.pilot@test.com";
    private static final String PASSWORD = "Password123";

    @BeforeAll
    static void setupAuth() {

        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (Exception ignored) {
        }

        safeRegister(OPERATOR_EMAIL, PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
        safeRegister(PILOT_EMAIL, PASSWORD, AISafeRoles.PILOT);
    }

    @BeforeEach
    void authenticate() {
        AuthzRegistry.authenticationService()
                .authenticate(OPERATOR_EMAIL, PASSWORD);
    }

    private ListWeatherDataController ctrl() {
        return new ListWeatherDataController();
    }

    @Test
    void ensureListAllDoesNotReturnNull() {

        Iterable<WeatherDataDTO> result = ctrl().listAll();

        assertNotNull(result);
    }

    @Test
    void ensureListAllReturnsIterable() {

        Iterable<WeatherDataDTO> result = ctrl().listAll();

        assertInstanceOf(Iterable.class, result);
    }

    @Test
    void ensureListAllReturnsDTOObjects() {

        Iterable<WeatherDataDTO> result = ctrl().listAll();

        for (Object obj : result) {
            assertInstanceOf(WeatherDataDTO.class, obj);
        }
    }

    private static void safeRegister(
            final String email,
            final String password,
            final eapli.framework.infrastructure.authz.domain.model.Role role) {

        try {
            AuthzRegistry.userService().registerNewUser(
                    email,
                    password,
                    "Test",
                    "User",
                    email,
                    Set.of(role));
        } catch (Exception ignored) {
        }
    }
}