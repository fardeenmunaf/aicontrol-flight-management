package flightPlan.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import shared.domain.FlightPlanId;

import java.time.Instant;

@Entity
public class FlightPlan implements AggregateRoot<FlightPlanId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightPlanId flightPlanId;

    @Enumerated(EnumType.STRING)
    private FlightPlanStatus status;

    // --- Promoted columns: duplicated from the JSON document so they can be queried/constrained
    //     in SQL. The flight_plan_json blob remains the source of truth. ---

    @Column(name = "aircraft_id")
    private String aircraftId;

    @Column(name = "pilot_ref")
    private String pilotRef;

    @Column(name = "route_reference")
    private String routeReference;

    @Column(name = "departure_time")
    private Instant departureTime;

    @Column(name = "flight_plan_json", columnDefinition = "TEXT")
    private String flightPlanJson;

    public FlightPlan(final FlightPlanId flightPlanId, final String flightPlanJson,
            final String aircraftId, final String pilotRef,
            final String routeReference, final Instant departureTime) {
        Preconditions.noneNull(flightPlanId, flightPlanJson);
        Preconditions.nonEmpty(flightPlanJson, "Flight plan JSON must not be empty");
        this.flightPlanId = flightPlanId;
        this.flightPlanJson = flightPlanJson;
        this.aircraftId = aircraftId;
        this.pilotRef = pilotRef;
        this.routeReference = routeReference;
        this.departureTime = departureTime;
        this.status = FlightPlanStatus.DRAFT;
    }

    protected FlightPlan() {
        // for ORM
    }

    public void markAsTested() {
        this.status = FlightPlanStatus.TESTED;
    }

    public void voidTest() {
        if (this.status != FlightPlanStatus.TESTED) {
            throw new IllegalStateException("Only a TESTED FlightPlan can be voided");
        }
        this.status = FlightPlanStatus.VOID;
    }

    public FlightPlanStatus status() {
        return status;
    }

    public String flightPlanJson() {
        return flightPlanJson;
    }

    public String aircraftId() {
        return aircraftId;
    }

    public String pilotRef() {
        return pilotRef;
    }

    public String routeReference() {
        return routeReference;
    }

    public Instant departureTime() {
        return departureTime;
    }

    @Override
    public FlightPlanId identity() {
        return flightPlanId;
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
}
