package flightRoute.domain;

import java.time.LocalDate;

import airport.domain.Airport;
import airtransportcompany.domain.AirTransportCompany;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import shared.domain.FlightRouteId;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;


@Entity
public class FlightRoute implements AggregateRoot<FlightRouteId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightRouteId flightRouteId;

    @Embedded
    private RouteDesignation designation;

    @Column(nullable = false)
    private LocalDate activeFrom;

    @Column(nullable = true)
    private LocalDate activeUntil;

    @ManyToOne(optional = false)
    private Airport originAirport;

    @ManyToOne(optional = false)
    private Airport destinationAirport;

    @ManyToOne(optional = false)
    private AirTransportCompany company;

    public FlightRoute(final FlightRouteId flightRouteId, final RouteDesignation designation,
                       final LocalDate activeFrom, final Airport originAirport,
                       final Airport destinationAirport, final AirTransportCompany company) {
        Preconditions.noneNull(flightRouteId, designation, activeFrom, originAirport, destinationAirport, company);
        Preconditions.ensure(!originAirport.identity().equals(destinationAirport.identity()),
                "Origin and destination airports must be different");
        this.flightRouteId      = flightRouteId;
        this.designation        = designation;
        this.activeFrom         = activeFrom;
        this.activeUntil        = null;
        this.originAirport      = originAirport;
        this.destinationAirport = destinationAirport;
        this.company            = company;
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
            throw new IllegalStateException(
                    "Cannot set deactivation to " + date + ": route is already deactivated on " + this.activeUntil);
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

    public void reactivate() {
        if (this.activeUntil == null) {
            throw new IllegalStateException("Route is already active");
        }
        this.activeUntil = null;
    }

    public Airport originAirport() {
        return originAirport;
    }

    public Airport destinationAirport() {
        return destinationAirport;
    }

    public AirTransportCompany company() {
        return company;
    }


    @Override
    public String toString() {
        return String.format("FlightRoute{designation=%s, activeFrom=%s, activeUntil=%s}",
                designation, activeFrom, activeUntil == null ? "active" : activeUntil);
    }
}