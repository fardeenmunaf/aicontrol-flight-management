package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;
import enginemodel.repositories.EngineModelRepository;
import shared.domain.EngineModelId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaEngineModelRepository
        extends BaseJpaRepositoryBase<EngineModel, EngineModelId, EngineModelId>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "engineModelId");
    }

    public JpaEngineModelRepository(final String persistenceUnitName) {
        super("engineModelId");
    }

    @Override
    public Optional<EngineModel> findByName(final String name) {
        return matchOne("LOWER(e.name) = LOWER(:name)", Map.of("name", name));
    }

    @Override
    public Optional<EngineModel> findByNameAndMakerName(final String name, final String makerName) {
        return matchOne(
                "LOWER(e.name) = LOWER(:name) AND LOWER(e.maker.name) = LOWER(:makerName)",
                Map.of("name", name, "makerName", makerName)
        );
    }

    @Override
    public List<EngineModel> findByType(final EngineType type) {
        return match("e.type = :type", Map.of("type", type));
    }
}