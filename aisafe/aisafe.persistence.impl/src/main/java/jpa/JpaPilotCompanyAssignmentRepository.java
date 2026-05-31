package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import pilot.domain.PilotCompanyAssignment;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.List;
import java.util.Map;

public class JpaPilotCompanyAssignmentRepository
        extends BaseJpaRepositoryBase<PilotCompanyAssignment, UserId, UserId> {

    public JpaPilotCompanyAssignmentRepository(final TransactionalContext autoTx) {
        super(autoTx, "pilotUserId");
    }

    public JpaPilotCompanyAssignmentRepository(final String persistenceUnitName) {
        super("pilotUserId");
    }

    public List<PilotCompanyAssignment> findByCompanyId(final AirTransportCompanyId companyId) {
        return match("e.companyId = :companyId", Map.of("companyId", companyId));
    }
}
