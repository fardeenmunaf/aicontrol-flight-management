package user.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import auth.domain.AISafeRoles;
import user.domain.User;
import user.repository.UserRepository;

import java.util.List;
import java.util.stream.StreamSupport;

public class ListUsersController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final UserRepository userRepository;

    public ListUsersController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> allUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);
        return StreamSupport
                .stream(userRepository.findAll().spliterator(), false)
                .toList();
    }
}