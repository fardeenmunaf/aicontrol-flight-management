package jpa.user.persistence;

import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import user.domain.User;
import user.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

public class JpaUserRepository
        extends JpaAutoTxRepository<User, Long, Long>
        implements UserRepository {

    public JpaUserRepository(final String persistenceUnitName) {
        super(persistenceUnitName, "id");
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return matchOne("e.email = :email", Map.of("email", email));
    }
}