package flightRoute.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.FlightId;


@Entity
public class FlightRoute implements AggregateRoot<FlightRouteId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightRouteId flightRouteId;

    @Embedded
    private RouteDesignation designation;

    private LocalDate activeFrom;

    /**
     * Null means the route is still active.
     * When set, the route is deactivated from that date onwards (US074).
     */
    private LocalDate activeUntil;

    /**
     * Cross-aggregate references to scheduled Flight instances.
     * Stored as plain FlightId values — DDD cross-aggregate reference rule.
     */
    @ElementCollection
    @JoinColumn(name = "flight_route_ref")
    private List<FlightId> scheduledFlightIds = new ArrayList<>();

    public FlightRoute(final FlightRouteId flightRouteId, final RouteDesignation designation,
                       final LocalDate activeFrom) {
        Preconditions.noneNull(flightRouteId, designation, activeFrom);
        this.flightRouteId = flightRouteId;
        this.designation   = designation;
        this.activeFrom    = activeFrom;
        this.activeUntil   = null; // active by default
    }

    protected FlightRoute() {
        // for ORM
    }

    /**
     * Deactivates the route from the given date onwards (US074 soft delete).
     * Cannot deactivate if scheduled flights exist after that date — that check
     * must be enforced at the application/service layer since it requires
     * querying the Flight repository.
     */
    public void deactivateFrom(final LocalDate date) {
        Preconditions.nonNull(date, "Deactivation date must not be null");
        Preconditions.ensure(!date.isBefore(activeFrom),
                "Deactivation date must be on or after the route's active-from date");
        if (this.activeUntil != null && !date.isAfter(this.activeUntil)) {
            throw new IllegalStateException("Route is already deactivated on or before " + date);
        }
        this.activeUntil = date;
    }

    /**
     * Returns true if this route is active on the given date.
     * Used by the application layer to guard flight creation (US073/US074).
     */
    public boolean isActiveOn(final LocalDate date) {
        Preconditions.nonNull(date, "Date must not be null");
        if (date.isBefore(activeFrom)) return false;
        return activeUntil == null || !date.isAfter(activeUntil);
    }

    /**
     * Schedules a new flight on this route.
     * Enforces that the route must be active on the flight's departure date.
     */
    public void scheduleFlightOn(final FlightId flightId, final LocalDate flightDate) {
        Preconditions.noneNull(flightId, flightDate);
        if (!isActiveOn(flightDate)) {
            throw new IllegalStateException(
                    "Cannot schedule a flight on route " + designation
                            + ": the route is not active on " + flightDate);
        }
        if (scheduledFlightIds.contains(flightId)) {
            throw new IllegalArgumentException("Flight " + flightId + " is already scheduled on this route");
        }
        scheduledFlightIds.add(flightId);
    }

    /**
     * Removes a scheduled flight — used when a flight is cancelled before departure.
     */
    public void removeScheduledFlight(final FlightId flightId) {
        Preconditions.nonNull(flightId, "FlightId must not be null");
        if (!scheduledFlightIds.remove(flightId)) {
            throw new IllegalArgumentException("Flight " + flightId + " is not scheduled on this route");
        }
    }

    @Override
    public FlightRouteId identity() {
        return flightRouteId;
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

    public RouteDesignation designation() {
        return designation;
    }

    public LocalDate activeFrom() {
        return activeFrom;
    }

    public LocalDate activeUntil() {
        return activeUntil;
    }

    public boolean isActive() {
        return isActiveOn(LocalDate.now());
    }

    public List<FlightId> scheduledFlightIds() {
        return Collections.unmodifiableList(scheduledFlightIds);
    }

    @Override
    public String toString() {
        return String.format("FlightRoute{designation=%s, activeFrom=%s, activeUntil=%s}",
                designation, activeFrom, activeUntil == null ? "active" : activeUntil);
    }
}