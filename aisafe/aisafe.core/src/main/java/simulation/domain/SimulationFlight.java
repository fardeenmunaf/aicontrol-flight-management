package simulation.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;
import aircontrolarea.domain.AirControlArea;

@Entity
public class SimulationFlight implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String callsign;
    private Instant departureDate;
    private boolean completed;
    private boolean hadViolation;

    @ManyToOne
    @JoinColumn(name = "air_control_area_id")
    private AirControlArea airControlArea;

    public SimulationFlight(final String callsign, final Instant departureDate,
            final boolean completed, final boolean hadViolation, final AirControlArea airControlArea) {
        Preconditions.noneNull(callsign, departureDate);
        if (callsign.isBlank()) {
            throw new IllegalArgumentException("Callsign cannot be blank");
        }
        this.callsign = callsign;
        this.departureDate = departureDate;
        this.completed = completed;
        this.hadViolation = hadViolation;
        this.airControlArea = airControlArea;
    }

    protected SimulationFlight() {
        // for ORM
    }

    /** Passed = completed the route with no safety violation. */
    public boolean passedValidation() {
        return completed && !hadViolation;
    }

    public String callsign() { return callsign; }
    public Instant departureDate() { return departureDate; }
    public boolean completed() { return completed; }
    public boolean hadViolation() { return hadViolation; }
    public AirControlArea airControlArea() { return airControlArea; }

    @Override public Long identity() { return id; }
    @Override public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SimulationFlight)) return false;
        final SimulationFlight other = (SimulationFlight) o;
        // When id is null (transient), fall back to object identity (already handled by == above)
        if (this.id == null || other.id == null) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Stable hash regardless of id assignment; use System.identityHashCode when transient
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
