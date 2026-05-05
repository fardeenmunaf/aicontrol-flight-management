package enginemodel.repositories;

import eapli.framework.domain.repositories.DomainRepository;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;
import shared.domain.EngineModelId;

import java.util.List;
import java.util.Optional;

public interface EngineModelRepository extends DomainRepository<EngineModelId, EngineModel> {

    Optional<EngineModel> findByName(String name);

    Optional<EngineModel> findByNameAndMakerName(String name, String makerName);

    List<EngineModel> findByType(EngineType type);
}