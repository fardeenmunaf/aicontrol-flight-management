package infrastructure.persistence;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.repositories.EngineModelRepository;
import user.repository.UserRepository;

/**
 * Master contract defining creation methods for all aggregate repositories.
 * Implementations are responsible for providing either JPA or In-Memory variants.
 *
 * @author LAPR4 Team
 */
public interface RepositoryFactory {

    /**
     * Factory method to create a transactional context for use in repositories.
     *
     * @return a new transactional context
     */
    TransactionalContext newTransactionalContext();

    /**
     * Get the EngineModel repository with auto-transaction management.
     *
     * @return the EngineModelRepository instance
     */
    EngineModelRepository engineModels();

    UserRepository users();

    /**
     * Get the EngineModel repository enrolled in a specific transactional context.
     *
     * @param autoTx the transactional context to enrol
     * @return the EngineModelRepository instance
     */
    EngineModelRepository engineModels(TransactionalContext autoTx);

    UserRepository users(TransactionalContext autoTx);
}
