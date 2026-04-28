package pilot.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.AircraftModelId;
import shared.domain.PilotId;
import shared.domain.UserId;

@Entity
public class Pilot implements AggregateRoot<PilotId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private PilotId pilotId;

    @Embedded
    private UserId userId;

    @Enumerated(EnumType.STRING)
    private PilotStatus status;

    @ElementCollection
    private Set<AircraftModelId> certifiedAircraftModelIds = new HashSet<>();

    public Pilot(final PilotId pilotId, final UserId userId, final PilotStatus status, final Set<AircraftModelId> certifiedAircraftModelIds) {
        Preconditions.noneNull(pilotId, userId, status, certifiedAircraftModelIds);
        this.pilotId = pilotId;
        this.userId = userId;
        this.status = status;
        this.certifiedAircraftModelIds.addAll(certifiedAircraftModelIds);
    }

    protected Pilot() {
        // for ORM
    }

    @Override
    public PilotId identity() {
        return pilotId;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    public UserId userId() {
        return userId;
    }

    public PilotStatus status() {
        return status;
    }

    public Set<AircraftModelId> certifiedAircraftModelIds() {
        return new HashSet<>(certifiedAircraftModelIds);
    }
}
