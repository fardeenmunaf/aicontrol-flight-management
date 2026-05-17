package user.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import user.domain.Role;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListUsersControllerTest extends UserApplicationTestSupport {

    private final StubUserRepository userRepository = new StubUserRepository();

    @BeforeAll
    static void init() {
        setupAuth();
    }

    @BeforeEach
    void resetState() {
        userRepository.clear();
        userRepository.add(user("list.one@aisafe.com", "List One", "ACTIVE", Set.of(Role.ADMIN)));
        userRepository.add(user("list.two@aisafe.com", "List Two", "DISABLED", Set.of(Role.PILOT)));
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authorizationService().clearSession();
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authenticationService().authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    void ensureListUsersReturnsAllPersistedUsers() {
        final List<user.domain.User> result = new ListUsersController(userRepository).allUsers();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.email().equals("list.one@aisafe.com")));
        assertTrue(result.stream().anyMatch(u -> u.email().equals("list.two@aisafe.com")));
    }

    @Test
    void ensureListUsersRejectsUnauthorizedUser() {
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authorizationService().clearSession();
        eapli.framework.infrastructure.authz.application.AuthzRegistry.authenticationService().authenticate(LOGIN_EMAIL, LOGIN_PASSWORD);

        assertThrows(RuntimeException.class, () -> new ListUsersController(userRepository).allUsers());
    }
}


