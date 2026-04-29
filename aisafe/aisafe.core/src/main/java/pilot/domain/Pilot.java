package pilot.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import aircraftmodel.domain.AircraftModel;
import user.domain.User;

@Entity
public class Pilot implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    @OneToOne(optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private PilotStatus status;

    @ManyToMany
    private Set<AircraftModel> certifiedAircraftModels = new HashSet<>();

    public Pilot(final User user, final PilotStatus status, final Set<AircraftModel> certifiedAircraftModels) {
        Preconditions.noneNull(user, status, certifiedAircraftModels);
        this.user = user;
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

    public User user() {
        return user;
    }

    public PilotStatus status() {
        return status;
    }

    public Set<AircraftModel> certifiedAircraftModels() {
        return new HashSet<>(certifiedAircraftModels);
    }
}
