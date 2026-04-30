package inmemory;

import enginemodel.domain.EngineModel;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import enginemodel.repositories.EngineModelRepository;

/**
 * In-memory implementation of EngineModelRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryEngineModelRepository extends InMemoryDomainRepository<EngineModel, Long>
        implements EngineModelRepository {

    static {
        InMemoryInitializer.init();
    }
}
