package flightPlan.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import shared.domain.FlightPlanId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class FlightPlan implements AggregateRoot<FlightPlanId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private FlightPlanId flightPlanId;

    @Enumerated(EnumType.STRING)
    private FlightPlanStatus status;

    private String flightDesignator;

    @Enumerated(EnumType.STRING)
    private FlightType type;

    private String aircraftId;
    private String pilotRef;

    @Column(name = "route_reference")
    private String routeReference;
    @Column(name = "route_origin")
    private String routeOrigin;
    @Column(name = "route_destination")
    private String routeDestination;

    // Denormalized first-leg departure, kept for route/date queries.
    @Column(name = "departure_time")
    private Instant departureTime;

    @Lob
    @Column(name = "dsl_text", columnDefinition = "TEXT")
    private String dslText;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "flight_plan_id")
    private List<FlightLeg> legs = new ArrayList<>();

    public FlightPlan(final FlightPlanId flightPlanId, final String flightDesignator,
                      final FlightType type, final String aircraftId, final String pilotRef,
                      final String routeReference, final String routeOrigin, final String routeDestination,
                      final List<FlightLeg> legs) {
        Preconditions.noneNull(flightPlanId, flightDesignator, type, aircraftId, pilotRef,
                routeReference, routeOrigin, routeDestination, legs);
        Preconditions.ensure(!legs.isEmpty(), "A flight plan must have at least one leg");
        this.flightPlanId = flightPlanId;
        this.flightDesignator = flightDesignator;
        this.type = type;
        this.aircraftId = aircraftId;
        this.pilotRef = pilotRef;
        this.routeReference = routeReference;
        this.routeOrigin = routeOrigin;
        this.routeDestination = routeDestination;
        this.legs = new ArrayList<>(legs);
        this.departureTime = legs.get(0).departureTime();
        this.status = FlightPlanStatus.DRAFT;
    }

    protected FlightPlan() {
        // for ORM
    }

    public void storeDslText(final String text) {
        this.dslText = text;
    }

    public String dslText() {
        return dslText;
    }

    public void markAsTested() {
        if (this.status != FlightPlanStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT FlightPlan can be marked as tested");
        }
        this.status = FlightPlanStatus.TESTED;
    }

    public void voidTest() {
        if (this.status != FlightPlanStatus.TESTED) {
            throw new IllegalStateException("Only a TESTED FlightPlan can be voided");
        }
        this.status = FlightPlanStatus.VOID;
    }

    public FlightPlanStatus status() { return status; }
    public String flightDesignator() { return flightDesignator; }
    public FlightType type() { return type; }
    public String aircraftId() { return aircraftId; }
    public String pilotRef() { return pilotRef; }
    public String routeReference() { return routeReference; }
    public String routeOrigin() { return routeOrigin; }
    public String routeDestination() { return routeDestination; }
    public Instant departureTime() { return departureTime; }
    public List<FlightLeg> legs() { return Collections.unmodifiableList(legs); }

    @Override
    public FlightPlanId identity() { return flightPlanId; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }
}
