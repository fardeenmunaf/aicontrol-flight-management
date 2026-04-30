package pilot.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import aircraftmodel.domain.AircraftModel;
import shared.domain.UserId;

@Entity
public class Pilot implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    @Embedded
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
    }

    protected Pilot() {
        // for ORM
    }

    @Override
    public Long identity() {
        return id;
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
