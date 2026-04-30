package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

/**
 * Base class for all JPA auto-transaction repositories.
 * Centralizes persistence unit name and extended properties configuration.
 *
 * @param <T> the entity type
 * @param <K> the domain identity type
 * @param <I> the persistent identity type
 *
 * @author LAPR4 Team
 */
abstract class BaseJpaRepositoryBase<T, K, I> extends JpaAutoTxRepository<T, K, I> {

    /**
     * Constructor for use with a shared transactional context.
     *
     * @param autoTx            the transactional context
     * @param identityFieldName the name of the identity field
     */
    protected BaseJpaRepositoryBase(final TransactionalContext autoTx, final String identityFieldName) {
        super(autoTx, identityFieldName);
    }

    /**
     * Constructor using default persistence unit from settings.
     *
     * @param identityFieldName the name of the identity field
     */
    protected BaseJpaRepositoryBase(final String identityFieldName) {
        super(PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties(),
                identityFieldName);
    }
}