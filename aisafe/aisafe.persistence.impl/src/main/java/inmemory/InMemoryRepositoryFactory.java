package inmemory;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.repositories.EngineModelRepository;
import user.repository.UserRepository;
import infrastructure.persistence.RepositoryFactory;

/**
 * In-memory implementation of RepositoryFactory.
 * Returns in-memory repository instances for all aggregates.
 * Suitable for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryRepositoryFactory implements RepositoryFactory {

    @Override
    public TransactionalContext newTransactionalContext() {
        // in-memory does not support transactions
        return null;
    }

    @Override
    public UserRepository users(final TransactionalContext autoTx) {
        return new InMemoryUserRepository();  // in-memory repositories ignore transactional context
    }

    @Override
    public UserRepository users() {
        return users(null); // delegate to the method with transactional context, passing null since it's not supported
    }

    @Override
    public EngineModelRepository engineModels(final TransactionalContext autoTx) {
        return new InMemoryEngineModelRepository();
    }

    @Override
    public EngineModelRepository engineModels() {
        return engineModels(null);
    }
}
