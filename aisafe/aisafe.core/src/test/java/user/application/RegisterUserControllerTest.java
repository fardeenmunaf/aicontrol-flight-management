package user.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import user.domain.Role;
import user.domain.User;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterUserControllerTest extends UserApplicationTestSupport {

    private final StubUserRepository userRepository = new StubUserRepository();

    @BeforeAll
    static void init() {
        setupAuth();
    }

    @BeforeEach
    void resetState() {
        userRepository.clear();
        userRepository.add(user("existing@aisafe.com", "Existing User", "ACTIVE", Set.of(Role.ADMIN)));
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authorizationService().clearSession();
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authenticationService().authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    void ensureRegisterUserCreatesDomainUserAndPersistsIt() {
        final User result = new RegisterUserController(userRepository).registerUser(
                "new.user@aisafe.com",
                "Password789",
                "New User",
                "+351912345678",
                Set.of(Role.ADMIN, Role.PILOT));

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("new.user@aisafe.com", result.email()),
                () -> assertEquals("New User", result.name()),
                () -> assertEquals("+351912345678", result.phoneNumber()),
                () -> assertEquals("ACTIVE", result.status()),
                () -> assertEquals(Set.of(Role.ADMIN, Role.PILOT), result.roles()),
                () -> assertEquals(result, userRepository.findByEmail("new.user@aisafe.com").orElseThrow())
        );
    }

    @Test
    void ensureRegisterUserSupportsSingleWordNames() {
        final User result = new RegisterUserController(userRepository).registerUser(
                "single.name@aisafe.com",
                "Password789",
                "Single",
                "+351912345679",
                Set.of(Role.BACKOFFICE_OPERATOR));

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("Single", result.name()),
                () -> assertEquals("single.name@aisafe.com", result.email())
        );
    }
}


