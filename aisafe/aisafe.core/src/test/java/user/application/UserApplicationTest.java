package user.application;

import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.UserId;
import user.domain.Role;
import user.domain.User;
import user.repository.UserRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

abstract class UserApplicationTestSupport {

    static final String ADMIN_EMAIL = "user.admin@aisafe.com";
    static final String ADMIN_PASSWORD = "Password123";

    static final String LOGIN_EMAIL = "login.user@aisafe.com";
    static final String LOGIN_PASSWORD = "Password456";

    protected static void setupAuth() {
        try {
            AuthzRegistry.configure(
                    new InMemoryUserRepository(),
                    new AISafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {
            // already configured by another test class
        }

        safeRegister(ADMIN_EMAIL, ADMIN_PASSWORD, AISafeRoles.ADMIN);
        safeRegister(LOGIN_EMAIL, LOGIN_PASSWORD, AISafeRoles.BACKOFFICE_OPERATOR);
    }

    protected static User user(final String email, final String name, final String status, final Set<Role> roles) {
        return new User(
                email,
                name,
                "+351900000000",
                null,
                null,
                status,
                roles,
                UserId.valueOf(email));
    }

    private static void safeRegister(final String email, final String password,
                                     final eapli.framework.infrastructure.authz.domain.model.Role role) {
        try {
            AuthzRegistry.userService().registerNewUser(email, password, "Test", "User", email, Set.of(role));
        } catch (final Exception ignored) {
            // user may already exist when tests are run repeatedly
        }
    }

    static class StubUserRepository
            extends InMemoryDomainRepository<User, UserId>
            implements UserRepository {

        void clear() {
            data().clear();
        }

        void add(final User user) {
            save(user);
        }

        @Override
        public Optional<User> findByEmail(final String email) {
            return matchOne(u -> u.email().equals(email));
        }

        @Override
        public Iterable<User> findAll() {
            return new ArrayList<>(data().values());
        }
    }
}




