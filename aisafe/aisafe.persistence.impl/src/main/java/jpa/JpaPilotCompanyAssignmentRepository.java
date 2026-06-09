package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import pilot.domain.PilotCompanyAssignment;
import pilot.repository.PilotCompanyAssignmentRepository;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaPilotCompanyAssignmentRepository
        extends BaseJpaRepositoryBase<PilotCompanyAssignment, UserId, UserId>
        implements PilotCompanyAssignmentRepository {

    public JpaPilotCompanyAssignmentRepository(final TransactionalContext autoTx) {
        super(autoTx, "pilotUserId");
    }

    public JpaPilotCompanyAssignmentRepository(final String persistenceUnitName) {
        super("pilotUserId");
    }

    @Override
    public Optional<PilotCompanyAssignment> findByPilotUserId(final UserId pilotUserId) {
        return matchOne("e.pilotUserId = :pilotUserId", Map.of("pilotUserId", pilotUserId));
    }

    @Override
    public List<PilotCompanyAssignment> findByCompanyId(final AirTransportCompanyId companyId) {
        return match("e.companyId = :companyId", Map.of("companyId", companyId));
    }
}
