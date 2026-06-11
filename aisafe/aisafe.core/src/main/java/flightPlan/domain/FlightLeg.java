package flightPlan.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;
import shared.domain.AirportId;

@Entity
public class FlightLeg implements DomainEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private int sequenceNumber;
    private Instant departureTime;
    private Instant arrivalTime;
    private double fuelQuantity;

    // Cross-aggregate references to Airport — stored as plain IDs (DDD rule)
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "departure_airport_id"))
    private AirportId departureAirportId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "arrival_airport_id"))
    private AirportId arrivalAirportId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "flight_leg_id")
    private List<Segment> segments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "flight_leg_id")
    private List<ProfileEntry> profileEntries = new ArrayList<>();

    public FlightLeg(final int sequenceNumber, final Instant departureTime, final Instant arrivalTime,
                     final double fuelQuantity, final AirportId departureAirportId,
                     final AirportId arrivalAirportId) {
        Preconditions.noneNull(departureTime, arrivalTime, departureAirportId, arrivalAirportId);
        Preconditions.ensure(sequenceNumber > 0, "Sequence number must be positive");
        if (arrivalTime.isBefore(departureTime) || arrivalTime.equals(departureTime)) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }
        Preconditions.ensure(fuelQuantity > 0, "Fuel quantity must be strictly positive");
        this.sequenceNumber = sequenceNumber;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.fuelQuantity = fuelQuantity;
        this.departureAirportId = departureAirportId;
        this.arrivalAirportId = arrivalAirportId;
    }

    protected FlightLeg() {
        // for ORM
    }

    public void addSegment(final Segment segment) {
        Preconditions.nonNull(segment, "Segment must not be null");
        this.segments.add(segment);
    }

    public void addProfileEntry(final ProfileEntry entry) {
        Preconditions.nonNull(entry, "ProfileEntry must not be null");
        this.profileEntries.add(entry);
    }

    public List<ProfileEntry> profileEntries() {
        return Collections.unmodifiableList(profileEntries);
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

    public int sequenceNumber() {
        return sequenceNumber;
    }

    public Instant departureTime() {
        return departureTime;
    }

    public Instant arrivalTime() {
        return arrivalTime;
    }

    public double fuelQuantity() {
        return fuelQuantity;
    }

    public AirportId departureAirportId() {
        return departureAirportId;
    }

    public AirportId arrivalAirportId() {
        return arrivalAirportId;
    }

    public List<Segment> segments() {
        return Collections.unmodifiableList(segments);
    }
}