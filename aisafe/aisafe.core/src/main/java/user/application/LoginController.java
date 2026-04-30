package user.application;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;

public class LoginController {

    public boolean authenticate(final String email, final String rawPassword) {
        return AuthzRegistry.authenticationService()
                .authenticate(email, rawPassword)
                .isPresent();
    }

    public void logout() {
        AuthzRegistry.authorizationService().clearSession();
    }
}