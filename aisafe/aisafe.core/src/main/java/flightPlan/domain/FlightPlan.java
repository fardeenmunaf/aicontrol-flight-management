package flightPlan.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.PilotId;
import shared.domain.AircraftId;


@Entity
public class FlightPlan implements AggregateRoot<FlightPlanId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightPlanId flightPlanId;

    private Instant departureDateTime;
    private double fuelQuantity;
    private int passengerLoad;
    private double cargoLoad;

    @Enumerated(EnumType.STRING)
    private FlightPlanStatus status;

    // Cross-aggregate references — stored as plain IDs (DDD rule)
    @Embedded
    private AircraftId aircraftId;

    @Embedded
    private PilotId pilotId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "flight_plan_ref")
    @OrderBy("sequenceNumber ASC")
    private List<FlightLeg> legs = new ArrayList<>();

    public FlightPlan(final FlightPlanId flightPlanId, final Instant departureDateTime,
                      final double fuelQuantity, final int passengerLoad, final double cargoLoad,
                      final AircraftId aircraftId, final PilotId pilotId) {
        Preconditions.noneNull(flightPlanId, departureDateTime, aircraftId, pilotId);
        Preconditions.ensure(fuelQuantity > 0, "Fuel quantity must be strictly positive");
        Preconditions.ensure(passengerLoad >= 0, "Passenger load must be non-negative");
        Preconditions.ensure(cargoLoad >= 0, "Cargo load must be non-negative");
        this.flightPlanId = flightPlanId;
        this.departureDateTime = departureDateTime;
        this.fuelQuantity = fuelQuantity;
        this.passengerLoad = passengerLoad;
        this.cargoLoad = cargoLoad;
        this.aircraftId = aircraftId;
        this.pilotId = pilotId;
        this.status = FlightPlanStatus.DRAFT;
    }

    protected FlightPlan() {
        // for ORM
    }

    /**
     * Adds a flight leg, enforcing the DSL semantic rule:
     * the arrival airport of leg N must match the departure airport of leg N+1.
     * Any structural change resets a TESTED plan back to DRAFT.
     */
    public void addLeg(final FlightLeg leg) {
        Preconditions.nonNull(leg, "FlightLeg must not be null");
        if (!legs.isEmpty()) {
            final FlightLeg lastLeg = legs.get(legs.size() - 1);
            if (!lastLeg.arrivalAirportId().equals(leg.departureAirportId())) {
                throw new IllegalArgumentException(
                        "Departure airport of leg " + leg.sequenceNumber()
                                + " must match arrival airport of leg " + lastLeg.sequenceNumber());
            }
        }
        if (this.status == FlightPlanStatus.TESTED) {
            this.status = FlightPlanStatus.DRAFT;
        }
        this.legs.add(leg);
    }

    /**
     * Marks the plan as tested after a successful validation run.
     */
    public void markAsTested() {
        if (legs.isEmpty()) {
            throw new IllegalStateException("A FlightPlan must have at least one leg before being tested");
        }
        this.status = FlightPlanStatus.TESTED;
    }

    /**
     * Voids a previously tested plan — e.g., when new weather data is inserted (US082).
     */
    public void voidTest() {
        if (this.status != FlightPlanStatus.TESTED) {
            throw new IllegalStateException("Only a TESTED FlightPlan can be voided");
        }
        this.status = FlightPlanStatus.VOID;
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

    public Instant departureDateTime() {
        return departureDateTime;
    }

    public double fuelQuantity() {
        return fuelQuantity;
    }

    public int passengerLoad() {
        return passengerLoad;
    }

    public double cargoLoad() {
        return cargoLoad;
    }

    public FlightPlanStatus status() {
        return status;
    }

    public AircraftId aircraftId() {
        return aircraftId;
    }

    public PilotId pilotId() {
        return pilotId;
    }

    public List<FlightLeg> legs() {
        return Collections.unmodifiableList(legs);
    }
}