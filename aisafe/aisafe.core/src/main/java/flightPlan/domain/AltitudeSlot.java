package flightPlan.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;

@Entity
public class AltitudeSlot implements DomainEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private double minimumAltitude;
    private double maximumAltitude;
    private double width;

    public AltitudeSlot(final double minimumAltitude, final double maximumAltitude, final double width) {
        Preconditions.ensure(minimumAltitude >= 0, "Minimum altitude must be non-negative");
        Preconditions.ensure(maximumAltitude > minimumAltitude,
                "Maximum altitude must be greater than minimum altitude");
        Preconditions.ensure(width > 0, "Width must be positive");
        this.minimumAltitude = minimumAltitude;
        this.maximumAltitude = maximumAltitude;
        this.width = width;
    }

    protected AltitudeSlot() {
        // for ORM
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

    public double minimumAltitude() {
        return minimumAltitude;
    }

    public double maximumAltitude() {
        return maximumAltitude;
    }

    public double width() {
        return width;
    }
}