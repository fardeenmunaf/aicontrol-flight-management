package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import pilot.domain.PilotCompanyAssignment;
import pilot.repository.PilotCompanyAssignmentRepository;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryPilotCompanyAssignmentRepository
        extends InMemoryDomainRepository<PilotCompanyAssignment, UserId>
        implements PilotCompanyAssignmentRepository {

    @Override
    public Optional<PilotCompanyAssignment> findByPilotUserId(final UserId pilotUserId) {
        return matchOne(e -> e.pilotUserId().equals(pilotUserId));
    }

    @Override
    public List<PilotCompanyAssignment> findByCompanyId(final AirTransportCompanyId companyId) {
        final List<PilotCompanyAssignment> result = new ArrayList<>();
        match(e -> e.companyId().equals(companyId)).forEach(result::add);
        return result;
    }
}
