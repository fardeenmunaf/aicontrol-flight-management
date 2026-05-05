package inmemory;

import aircraftmodel.domain.AircraftModel;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import pilot.repository.PilotRepository;
import shared.domain.PilotId;

import java.util.ArrayList;
import java.util.List;

public class InMemoryPilotRepository extends InMemoryDomainRepository<Pilot, PilotId> implements PilotRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public List<Pilot> findByStatus(final PilotStatus status) {
        final List<Pilot> result = new ArrayList<>();
        match(e -> e.status() == status).forEach(result::add);
        return result;
    }

    @Override
    public List<Pilot> findCertifiedFor(final AircraftModel aircraftModel) {
        final List<Pilot> result = new ArrayList<>();
        match(e -> e.certifiedAircraftModels().contains(aircraftModel)).forEach(result::add);
        return result;
    }
}