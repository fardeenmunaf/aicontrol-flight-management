package simulation.domain;

import java.time.Instant;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;

@Entity
public class AircraftPosition implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Simulation simulation;

    private Instant timestamp;
    private Double altitude;

    @Embedded
    private VelocityVector velocityVector;

    // TODO: Aircraft aggregate is not implemented yet.
    // private Aircraft aircraft;

    public AircraftPosition(final Simulation simulation,
            final Instant timestamp, final Double altitude,
            final VelocityVector velocityVector) {
        Preconditions.noneNull(simulation, timestamp, altitude, velocityVector);
        if (altitude < 0) {
            throw new IllegalArgumentException("Altitude must be non-negative");
        }
        this.simulation = simulation;
        this.timestamp = timestamp;
        this.altitude = altitude;
        this.velocityVector = velocityVector;
    }

    protected AircraftPosition() {
        // for ORM
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public Long identity() {
        return id;
    }

    public Simulation simulation() {
        return simulation;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Double altitude() {
        return altitude;
    }

    public VelocityVector velocityVector() {
        return velocityVector;
    }
}
