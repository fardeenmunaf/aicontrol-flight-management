package auth.infrastructure;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;

public class AISafePasswordPolicy implements PasswordPolicy {

    @Override
    public boolean isSatisfiedBy(final String rawPassword) {
        // minimum 8 characters
        return rawPassword != null && rawPassword.length() >= 8;
    }
}