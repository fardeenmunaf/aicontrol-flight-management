package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import user.domain.User;
import user.repository.UserRepository;
import shared.domain.UserId;

import java.util.Map;
import java.util.Optional;

public class JpaUserRepository
        extends BaseJpaRepositoryBase<User, UserId, UserId>
        implements UserRepository {

    public JpaUserRepository(final TransactionalContext autoTx) {
        super(autoTx, "userId");
    }

    public JpaUserRepository(final String persistenceUnitName) {
        super("userId");
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return matchOne("e.email = :email", Map.of("email", email));
    }
}