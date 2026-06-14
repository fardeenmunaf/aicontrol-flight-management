package simulation.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;

import aircraft.domain.Aircraft;
import aircontrolarea.domain.AirControlArea;

@Entity
public class SafetyViolation implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "simulation_id")
    private Simulation simulation;

    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", length = 50)
    private ViolationType violationType;

    @ManyToOne
    @JoinColumn(name = "air_control_area_id")
    private AirControlArea airControlArea;

    @ElementCollection
    @CollectionTable(name = "SafetyViolation_involvedCallsigns",
            joinColumns = @JoinColumn(name = "safety_violation_id"))
    @Column(name = "callsign")
    private Set<String> involvedCallsigns = new HashSet<>();

    @ElementCollection
    private Set<VelocityVector> recordedVectors = new HashSet<>();

    @ManyToMany
    private Set<Aircraft> involvedAircraft = new HashSet<>();

    /** Full constructor with velocity vectors and Aircraft domain objects. */
    public SafetyViolation(final Simulation simulation, final Instant timestamp,
            final ViolationType violationType, final AirControlArea airControlArea,
            final Set<VelocityVector> recordedVectors, final Set<Aircraft> involvedAircraft) {
        Preconditions.noneNull(simulation, timestamp, violationType, recordedVectors, involvedAircraft);
        this.simulation = simulation;
        this.timestamp = timestamp;
        this.violationType = violationType;
        this.airControlArea = airControlArea;
        this.recordedVectors.addAll(recordedVectors);
        this.involvedAircraft.addAll(involvedAircraft);
    }

    /** Convenience constructor for recorded runs: type + callsigns, no vectors/Aircraft objects. */
    public SafetyViolation(final Simulation simulation, final Instant timestamp,
            final ViolationType violationType, final AirControlArea airControlArea,
            final Set<String> involvedCallsigns) {
        this(simulation, timestamp, violationType, airControlArea, new HashSet<>(), new HashSet<>());
        if (involvedCallsigns != null) this.involvedCallsigns.addAll(involvedCallsigns);
    }

    /** Minimal convenience constructor (no callsigns/vectors/aircraft). */
    public SafetyViolation(final Simulation simulation, final Instant timestamp,
            final ViolationType violationType, final AirControlArea airControlArea) {
        this(simulation, timestamp, violationType, airControlArea, new HashSet<>(), new HashSet<>());
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
        if (!(other instanceof SafetyViolation)) return false;
        final SafetyViolation o = (SafetyViolation) other;
        return timestamp.equals(o.timestamp)
                && violationType == o.violationType
                && simulation.equals(o.simulation);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyViolation)) return false;
        final SafetyViolation other = (SafetyViolation) o;
        // When transient (id not yet assigned), each instance is distinct.
        if (this.id == null || other.id == null) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Stable hash: use identity hash when transient so transient instances
        // never collide in a HashSet before being persisted.
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    public Simulation simulation()            { return simulation; }
    public Instant timestamp()               { return timestamp; }
    public ViolationType violationType()     { return violationType; }
    public AirControlArea airControlArea()   { return airControlArea; }
    public Set<String> involvedCallsigns()   { return new HashSet<>(involvedCallsigns); }
    public Set<VelocityVector> recordedVectors() { return new HashSet<>(recordedVectors); }
    public Set<Aircraft> involvedAircraft()  { return new HashSet<>(involvedAircraft); }
}
