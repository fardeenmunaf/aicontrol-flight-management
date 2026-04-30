package user.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Username;

import auth.domain.AISafeRoles;
import user.repository.UserRepository;

public class DisableEnableUserController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final UserRepository userRepository;

    public DisableEnableUserController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void disableUser(final String email) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);
        AuthzRegistry.userService()
                .userOfIdentity(Username.valueOf(email))
                .ifPresent(AuthzRegistry.userService()::deactivateUser);
        userRepository.findByEmail(email).ifPresent(u -> {
            u.disable();
            userRepository.save(u);
        });
    }

    public void enableUser(final String email) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN);
        AuthzRegistry.userService()
                .userOfIdentity(Username.valueOf(email))
                .ifPresent(AuthzRegistry.userService()::activateUser);
        userRepository.findByEmail(email).ifPresent(u -> {
            u.enable();
            userRepository.save(u);
        });
    }
}