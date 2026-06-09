package jpa;

import aircraftmodel.domain.AircraftModel;
import eapli.framework.domain.repositories.TransactionalContext;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import pilot.repository.PilotRepository;
import shared.domain.PilotId;
import shared.domain.UserId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaPilotRepository extends BaseJpaRepositoryBase<Pilot, PilotId, PilotId> implements PilotRepository {

    public JpaPilotRepository(final TransactionalContext autoTx) {
        super(autoTx, "pilotId");
    }

    public JpaPilotRepository(final String persistenceUnitName) {
        super("pilotId");
    }

    @Override
    public List<Pilot> findByStatus(final PilotStatus status) {
        return match("e.status = :status", Map.of("status", status));
    }

    @Override
    public List<Pilot> findCertifiedFor(final AircraftModel aircraftModel) {
        return match("?1 MEMBER OF e.certifiedAircraftModels", Map.of(1, aircraftModel));
    }

    @Override
    public Optional<Pilot> findByUserId(final UserId userId) {
        return matchOne("e.userId = :userId", Map.of("userId", userId));
    }
}