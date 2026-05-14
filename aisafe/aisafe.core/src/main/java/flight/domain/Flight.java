package flight.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import shared.domain.FlightId;
import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;

@Entity
public class Flight implements AggregateRoot<FlightId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightId flightId;

    private String flightDesignator;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private FlightType type;
    private Instant scheduledDepartureTime;
    private Instant scheduledArrivalTime;
    @ManyToOne
    private Aircraft aircraft;

    public Flight(final String flightDesignator, final FlightType type, final Instant scheduledDepartureTime, final Instant scheduledArrivalTime) {
        Preconditions.noneNull(type, scheduledDepartureTime, scheduledArrivalTime);
        if (StringPredicates.isNullOrEmpty(flightDesignator)) {
            throw new IllegalArgumentException("Flight designator should neither be null nor empty");
        }
        if (scheduledArrivalTime.isBefore(scheduledDepartureTime)) {
            throw new IllegalArgumentException("Scheduled arrival time must be after departure time");
        }
        this.flightDesignator = flightDesignator;
        this.type = type;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.flightId = FlightId.newId();
    }

    /**
     * Associate an aircraft with this flight.
     * Used by application layer when assigning an aircraft to a flight.
     */
    public void assignAircraft(final Aircraft aircraft) {
        if (aircraft == null) {
            throw new IllegalArgumentException("Assigned aircraft cannot be null");
        }
        if (aircraft.operationStatus() == AircraftOperationStatus.DECOMISSIONED) {
            throw new IllegalStateException("Cannot assign a decommissioned aircraft to a flight");
        }
        this.aircraft = aircraft;
    }

    protected Flight() {
        // for ORM
    }

    @Override
    public FlightId identity() {
        return flightId;
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

    public String flightDesignator() {
        return flightDesignator;
    }

    public FlightType type() {
        return type;
    }

    public Instant scheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public Instant scheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public Aircraft aircraft() {
        return aircraft;
    }
}