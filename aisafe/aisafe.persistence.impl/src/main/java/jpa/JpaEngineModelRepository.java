package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;

/**
 * JPA implementation of EngineModelRepository.
 * Provides ORM-based persistence for EngineModel aggregates.
 *
 * @author LAPR4 Team
 */
class JpaEngineModelRepository
        extends BaseJpaRepositoryBase<EngineModel, Long, Long>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaEngineModelRepository(final String persistenceUnitName) {
        super("id");
    }
}