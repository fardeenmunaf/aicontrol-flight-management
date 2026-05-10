package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import user.domain.User;
import user.repository.UserRepository;
import shared.domain.UserId;
import java.util.Optional;

/**
 * In-memory implementation of UserRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryUserRepository extends InMemoryDomainRepository<User, UserId>
        implements UserRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return matchOne(u -> u.email().equals(email));
    }

    @Override
    public User save(final User user) {
        // AC03: Check for duplicate email
        final Optional<User> existing = findByEmail(user.email());
        if (existing.isPresent() && !existing.get().userId().equals(user.userId())) {
            throw new IllegalArgumentException("Email " + user.email() + " is already registered");
        }
        return super.save(user);
    }
}