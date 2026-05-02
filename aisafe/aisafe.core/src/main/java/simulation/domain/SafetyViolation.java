package simulation.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;

import aircraft.domain.Aircraft;

@Entity
public class SafetyViolation implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Simulation simulation;

    private Instant timestamp;
    private String severity;

    @ElementCollection
    private Set<VelocityVector> recordedVectors = new HashSet<>();

    @ManyToMany
    private Set<Aircraft> involvedAircraft = new HashSet<>();

    public SafetyViolation(final Simulation simulation, final Instant timestamp, final String severity, final Set<VelocityVector> recordedVectors, final Set<Aircraft> involvedAircraft) 
    {
        Preconditions.noneNull(simulation, timestamp, severity, recordedVectors, involvedAircraft);
        if (recordedVectors.size() < 2) 
        {
            throw new IllegalArgumentException("At least two velocity vectors are required");
        }
        if (involvedAircraft.size() < 2) 
        {
            throw new IllegalArgumentException("At least two aircraft must be involved");
        }
        this.simulation = simulation;
        this.timestamp = timestamp;
        this.severity = severity;
        this.recordedVectors.addAll(recordedVectors);
        this.involvedAircraft.addAll(involvedAircraft);
}

    protected SafetyViolation() {
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

    public Simulation simulation() {
        return simulation;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String severity() {
        return severity;
    }

    public Set<VelocityVector> recordedVectors() {
        return new HashSet<>(recordedVectors);
    }

    public Set<Aircraft> involvedAircraft() {
        return new HashSet<>(involvedAircraft);
    }
}