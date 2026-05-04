package jpa;

import aircraftmodel.domain.AircraftModel;
import eapli.framework.domain.repositories.TransactionalContext;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import pilot.repository.PilotRepository;

import java.util.List;
import java.util.Map;

public class JpaPilotRepository extends BaseJpaRepositoryBase<Pilot, Long, Long> implements PilotRepository {

    public JpaPilotRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaPilotRepository(final String persistenceUnitName) {
        super("id");
    }

    @Override
    public List<Pilot> findByStatus(final PilotStatus status) {
        return match("e.status = :status", Map.of("status", status));
    }

    @Override
    public List<Pilot> findCertifiedFor(final AircraftModel aircraftModel) {
        return match("?1 MEMBER OF e.certifiedAircraftModels", Map.of(1, aircraftModel));
    }
}