package jpa;

import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.repositories.CustomerCollaboratorRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.UserId;

import java.util.Map;
import java.util.Optional;

public class JpaCustomerCollaboratorRepository
        extends BaseJpaRepositoryBase<CustomerCollaborator, CustomerCollaboratorId, CustomerCollaboratorId>
        implements CustomerCollaboratorRepository {

    public JpaCustomerCollaboratorRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaCustomerCollaboratorRepository(final String persistenceUnitName) {
        super("id");
    }

    @Override
    public Optional<CustomerCollaborator> findByUserId(final UserId userId) {
        return matchOne("e.userId = :userId", Map.of("userId", userId));
    }

    @Override
    public Iterable<CustomerCollaborator> findActiveByAirTransportCompanyId(final AirTransportCompanyId id) {
        return match("e.airTransportCompanyId = :id and e.active = true", Map.of("id", id));
    }

    @Override
    public Iterable<CustomerCollaborator> findActiveByAirControlAreaId(final AirControlAreaId id) {
        return match("e.airControlAreaId = :id and e.active = true", Map.of("id", id));
    }
}