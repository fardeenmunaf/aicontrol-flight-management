package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import user.repository.UserRepository;
import enginemodel.repositories.EngineModelRepository;
import infrastructure.persistence.RepositoryFactory;

/**
 * JPA implementation of RepositoryFactory.
 * Returns JPA-backed repository instances for all aggregates.
 * Suitable for production and integration testing environments.
 * Manages transactional contexts for auto-transaction management.
 *
 * @author LAPR4 Team
 */
public class JpaRepositoryFactory implements RepositoryFactory {

    @Override
    public TransactionalContext newTransactionalContext() {
        return JpaAutoTxRepository.buildTransactionalContext(PersistenceSettings.getPersistenceUnitName(), PersistenceSettings.getExtendedPersistenceProperties());
    }

    @Override
    public UserRepository users() {
        return new JpaUserRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public UserRepository users(final TransactionalContext autoTx) {
        return new JpaUserRepository(autoTx);
    }

    @Override
    public EngineModelRepository engineModels() {
        return new JpaEngineModelRepository(PersistenceSettings.getPersistenceUnitName());
    }

    @Override
    public EngineModelRepository engineModels(final TransactionalContext autoTx) {
        return new JpaEngineModelRepository(autoTx);
    }
}
