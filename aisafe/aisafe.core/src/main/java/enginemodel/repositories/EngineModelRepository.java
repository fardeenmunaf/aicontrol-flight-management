package enginemodel.repositories;

import eapli.framework.domain.repositories.DomainRepository;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;

import java.util.List;
import java.util.Optional;

public interface EngineModelRepository extends DomainRepository<Long, EngineModel> {

    Optional<EngineModel> findByName(String name);

    List<EngineModel> findByType(EngineType type);
}