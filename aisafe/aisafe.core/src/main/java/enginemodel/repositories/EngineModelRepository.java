package enginemodel.repositories;

import eapli.framework.domain.repositories.DomainRepository;
import enginemodel.domain.EngineModel;

/**
 * Repository interface for EngineModel aggregate.
 * Defines domain-specific persistence operations for EngineModel entities.
 *
 * @author LAPR4 Team
 */
public interface EngineModelRepository extends DomainRepository<Long, EngineModel> {
    // Domain-specific query methods can be added here as needed
}
