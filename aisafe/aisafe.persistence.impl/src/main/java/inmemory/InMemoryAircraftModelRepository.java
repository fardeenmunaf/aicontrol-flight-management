package inmemory;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.AircraftModelId;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAircraftModelRepository
    extends InMemoryDomainRepository<AircraftModel, AircraftModelId>
        implements AircraftModelRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public List<AircraftModel> findByType(final AircraftType type) {
        final List<AircraftModel> result = new ArrayList<>();
        match(a -> a.type() == type).forEach(result::add);
        return result;
    }

    @Override
    public List<AircraftModel> findByMakerName(final String makerName) {
        final List<AircraftModel> result = new ArrayList<>();
        match(a -> a.maker().name().equalsIgnoreCase(makerName)).forEach(result::add);
        return result;
    }
}