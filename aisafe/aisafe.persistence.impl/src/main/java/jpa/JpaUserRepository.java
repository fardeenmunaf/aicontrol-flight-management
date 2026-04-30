package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import user.domain.User;
import user.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

public class JpaUserRepository
        extends BaseJpaRepositoryBase<User, Long, Long>
        implements UserRepository {

    public JpaUserRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaUserRepository(final String persistenceUnitName) {
        super("id");
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return matchOne("e.email = :email", Map.of("email", email));
    }
}