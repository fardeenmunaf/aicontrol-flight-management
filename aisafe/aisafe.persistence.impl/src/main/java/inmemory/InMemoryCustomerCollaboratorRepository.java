package inmemory;

import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryCustomerCollaboratorRepository
        extends InMemoryDomainRepository<CustomerCollaborator, CustomerCollaboratorId>
        implements CustomerCollaboratorRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<CustomerCollaborator> findByUserId(final UserId userId) {
        return data().values().stream()
                .filter(c -> c.userId().equals(userId))
                .findFirst();
    }

    @Override
    public Iterable<CustomerCollaborator> findActiveByAirTransportCompanyId(final AirTransportCompanyId id) {
        final List<CustomerCollaborator> result = new ArrayList<>();
        for (final CustomerCollaborator collaborator : data().values()) {
            if (collaborator.isActive()
                    && collaborator.airTransportCompanyId() != null
                    && collaborator.airTransportCompanyId().equals(id)) {
                result.add(collaborator);
            }
        }
        return result;
    }

    @Override
    public Iterable<CustomerCollaborator> findActiveByAirControlAreaId(final AirControlAreaId id) {
        final List<CustomerCollaborator> result = new ArrayList<>();
        for (final CustomerCollaborator collaborator : data().values()) {
            if (collaborator.isActive()
                    && collaborator.airControlAreaId() != null
                    && collaborator.airControlAreaId().equals(id)) {
                result.add(collaborator);
            }
        }
        return result;
    }
}