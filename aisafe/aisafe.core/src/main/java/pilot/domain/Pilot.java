package pilot.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import aircraftmodel.domain.AircraftModel;
import shared.domain.PilotId;
import shared.domain.UserId;

@Entity
public class Pilot implements AggregateRoot<PilotId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @jakarta.persistence.AttributeOverride(name = "value", column = @jakarta.persistence.Column(name = "pilot_id"))
    private PilotId pilotId;

    @Embedded
    @jakarta.persistence.AttributeOverride(name = "value", column = @jakarta.persistence.Column(name = "user_id"))
    private UserId userId;

    @Enumerated(EnumType.STRING)
    private PilotStatus status;

    @ManyToMany
    private Set<AircraftModel> certifiedAircraftModels = new HashSet<>();

    public Pilot(final UserId userId, final PilotStatus status, final Set<AircraftModel> certifiedAircraftModels) {
        Preconditions.noneNull(userId, status, certifiedAircraftModels);
        this.userId = userId;
        this.status = status;
        this.certifiedAircraftModels.addAll(certifiedAircraftModels);
        this.pilotId = PilotId.newId();
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

    public Set<AircraftModel> certifiedAircraftModels() {
        return new HashSet<>(certifiedAircraftModels);
    }
}
