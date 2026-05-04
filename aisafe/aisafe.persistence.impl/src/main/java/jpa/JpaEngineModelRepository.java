package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;
import enginemodel.repositories.EngineModelRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaEngineModelRepository
        extends BaseJpaRepositoryBase<EngineModel, Long, Long>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaEngineModelRepository(final String persistenceUnitName) {
        super("id");
    }

    @Override
    public Optional<EngineModel> findByName(final String name) {
        return matchOne("LOWER(e.name) = LOWER(:name)", Map.of("name", name));
    }

    @Override
    public List<EngineModel> findByType(final EngineType type) {
        return match("e.type = :type", Map.of("type", type));
    }
}