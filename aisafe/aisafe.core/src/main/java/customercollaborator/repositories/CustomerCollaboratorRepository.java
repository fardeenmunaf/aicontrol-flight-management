package customercollaborator.repositories;

import customercollaborator.domain.CustomerCollaborator;
import eapli.framework.domain.repositories.DomainRepository;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.UserId;

import java.util.Optional;

public interface CustomerCollaboratorRepository extends DomainRepository<CustomerCollaboratorId, CustomerCollaborator> {

    Optional<CustomerCollaborator> findByUserId(UserId userId);

    Iterable<CustomerCollaborator> findActiveByAirTransportCompanyId(AirTransportCompanyId id);

    Iterable<CustomerCollaborator> findActiveByAirControlAreaId(AirControlAreaId id);
}