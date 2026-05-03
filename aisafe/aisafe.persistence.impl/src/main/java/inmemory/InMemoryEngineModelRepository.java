package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;
import enginemodel.repositories.EngineModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryEngineModelRepository
        extends InMemoryDomainRepository<EngineModel, Long>
        implements EngineModelRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<EngineModel> findByName(final String name) {
        return matchOne(e -> e.name().equalsIgnoreCase(name));
    }

    @Override
    public List<EngineModel> findByType(final EngineType type) {
        final List<EngineModel> result = new ArrayList<>();
        match(e -> e.type() == type).forEach(result::add);
        return result;
    }
}