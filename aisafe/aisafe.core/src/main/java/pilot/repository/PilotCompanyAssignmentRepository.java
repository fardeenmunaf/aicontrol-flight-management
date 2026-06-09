package pilot.repository;

import eapli.framework.domain.repositories.DomainRepository;
import pilot.domain.PilotCompanyAssignment;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.List;
import java.util.Optional;

public interface PilotCompanyAssignmentRepository extends DomainRepository<UserId, PilotCompanyAssignment> {

    Optional<PilotCompanyAssignment> findByPilotUserId(UserId pilotUserId);

    List<PilotCompanyAssignment> findByCompanyId(AirTransportCompanyId companyId);
}
