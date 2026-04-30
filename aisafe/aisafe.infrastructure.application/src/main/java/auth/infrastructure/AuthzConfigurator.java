package auth.infrastructure;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;

import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;

public final class AuthzConfigurator {

    public static void configure(final UserRepository userRepo) {
        AuthzRegistry.configure(
                userRepo,
                new AISafePasswordPolicy(),
                new PlainTextEncoder()
        );
    }

    private AuthzConfigurator() {}
}