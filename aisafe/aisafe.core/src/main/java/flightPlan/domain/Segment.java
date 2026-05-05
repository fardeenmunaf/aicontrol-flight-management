package flightPlan.domain;

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
import shared.domain.NodeId;

@Entity
public class Segment implements DomainEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Wind direction in degrees relative to North (0-359)
    private double windDirection;
    // Wind speed in m/s
    private double windSpeed;

    // Cross-aggregate references to Node — stored as plain IDs (DDD rule)
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "start_node_id"))
    private NodeId startNodeId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "end_node_id"))
    private NodeId endNodeId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "segment_id")
    private List<AltitudeSlot> altitudeSlots = new ArrayList<>();

    public Segment(final NodeId startNodeId, final NodeId endNodeId,
                   final double windDirection, final double windSpeed) {
        Preconditions.noneNull(startNodeId, endNodeId);
        if (startNodeId.equals(endNodeId)) {
            throw new IllegalArgumentException("Segment start and end nodes must be different");
        }
        Preconditions.ensure(windDirection >= 0 && windDirection < 360,
                "Wind direction must be between 0 (inclusive) and 360 (exclusive) degrees");
        Preconditions.ensure(windSpeed >= 0, "Wind speed must be non-negative");
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
    }

    protected Segment() {
        // for ORM
    }

    public void addAltitudeSlot(final AltitudeSlot altitudeSlot) {
        Preconditions.nonNull(altitudeSlot, "AltitudeSlot must not be null");
        this.altitudeSlots.add(altitudeSlot);
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

    public NodeId startNodeId() {
        return startNodeId;
    }

    public NodeId endNodeId() {
        return endNodeId;
    }

    public double windDirection() {
        return windDirection;
    }

    public double windSpeed() {
        return windSpeed;
    }

    public List<AltitudeSlot> altitudeSlots() {
        return Collections.unmodifiableList(altitudeSlots);
    }
}