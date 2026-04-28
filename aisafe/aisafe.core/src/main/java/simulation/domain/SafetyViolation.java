package simulation.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.AircraftId;

@Entity
public class SafetyViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Simulation simulation;

    private Instant timestamp;
    private String severity;

    @ElementCollection
    private Set<VelocityVector> recordedVectors = new HashSet<>();

    @ElementCollection
    // TODO: AircraftId will be implemented by another team; compilation will fail until then.
    private Set<AircraftId> involvedAircraftIds = new HashSet<>();

    public SafetyViolation(final Simulation simulation, final Instant timestamp, final String severity, final Set<VelocityVector> recordedVectors, final Set<AircraftId> involvedAircraftIds) {
        Preconditions.noneNull(simulation, timestamp, severity, recordedVectors, involvedAircraftIds);
        if (recordedVectors.size() < 2) {
            throw new IllegalArgumentException("At least two velocity vectors are required");
        }
        if (involvedAircraftIds.isEmpty()) {
            throw new IllegalArgumentException("At least one aircraft is required");
        }
        this.simulation = simulation;
        this.timestamp = timestamp;
        this.severity = severity;
        this.recordedVectors.addAll(recordedVectors);
        this.involvedAircraftIds.addAll(involvedAircraftIds);
    }

    protected SafetyViolation() {
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

    public Set<AircraftId> involvedAircraftIds() {
        return new HashSet<>(involvedAircraftIds);
    }
}
