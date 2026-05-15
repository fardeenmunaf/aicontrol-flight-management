package user.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import user.domain.Role;
import user.domain.User;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DisableEnableUserControllerTest extends UserApplicationTestSupport {

    private final StubUserRepository userRepository = new StubUserRepository();

    @BeforeAll
    static void init() {
        setupAuth();
    }

    @BeforeEach
    void resetState() {
        userRepository.clear();
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authorizationService().clearSession();
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authenticationService().authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    void ensureDisableAndEnableUpdatePersistedUserStatus() {
        final User user = user("toggle@aisafe.com", "Toggle User", "ACTIVE", Set.of(Role.PILOT));
        userRepository.add(user);

        final DisableEnableUserController controller = new DisableEnableUserController(userRepository);

        controller.disableUser("toggle@aisafe.com");
        assertFalse(userRepository.findByEmail("toggle@aisafe.com").orElseThrow().isActive());

        controller.enableUser("toggle@aisafe.com");
        assertTrue(userRepository.findByEmail("toggle@aisafe.com").orElseThrow().isActive());
    }

    @Test
    void ensureDisableAndEnableIgnoreMissingUsersWithoutFailing() {
        final DisableEnableUserController controller = new DisableEnableUserController(userRepository);

        assertDoesNotThrow(() -> controller.disableUser("missing@aisafe.com"));
        assertDoesNotThrow(() -> controller.enableUser("missing@aisafe.com"));
    }

    @Test
    void ensureAllUsersReturnsAllPersistedUsers() {
        final User user1 = user("allusers1@aisafe.com", "User One", "ACTIVE", Set.of(Role.PILOT));
        final User user2 = user("allusers2@aisafe.com", "User Two", "ACTIVE", Set.of(Role.PILOT));
        userRepository.add(user1);
        userRepository.add(user2);

        final DisableEnableUserController controller = new DisableEnableUserController(userRepository);
        final java.util.List<User> all = controller.allUsers();
        assertEquals(2, all.size());
    }

    @Test
    void ensureAllUsersWithEmptyRepositoryReturnsEmptyList() {
        final DisableEnableUserController controller = new DisableEnableUserController(userRepository);
        final java.util.List<User> all = controller.allUsers();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }
}


