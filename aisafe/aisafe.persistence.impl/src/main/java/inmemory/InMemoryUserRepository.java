package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import user.domain.User;
import user.repository.UserRepository;
import java.util.Optional;

/**
 * In-memory implementation of UserRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryUserRepository extends InMemoryDomainRepository<User, Long>
        implements UserRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return matchOne(u -> u.email().equals(email));
    }
}