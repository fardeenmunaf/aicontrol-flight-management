package flightPlan.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;

/**
 * One line of a flight leg's profile (a climb/cruise/descend point).
 * Cruise has no altitude and no rate; climb has a positive rate; descend has a negative rate.
 */
@Entity
public class ProfileEntry implements DomainEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProfilePhase phase;

    private Double altitude;          // null for cruise
    private String altitudeUnit;      // null for cruise
    private double speed;
    private String speedUnit;
    private Double rate;              // null except descend
    private String rateUnit;          // null except descend

    private ProfileEntry(final ProfilePhase phase, final Double altitude, final String altitudeUnit,
                         final double speed, final String speedUnit,
                         final Double rate, final String rateUnit) {
        Preconditions.nonNull(phase, "phase must not be null");
        Preconditions.ensure(speed > 0, "Profile speed must be strictly positive");
        if (rate != null && phase == ProfilePhase.DESCEND)
            Preconditions.ensure(rate < 0, "Descend rate must be negative");
        if (rate != null && phase == ProfilePhase.CLIMB)
            Preconditions.ensure(rate > 0, "Climb rate must be positive");
        this.phase = phase;
        this.altitude = altitude;
        this.altitudeUnit = altitudeUnit;
        this.speed = speed;
        this.speedUnit = speedUnit;
        this.rate = rate;
        this.rateUnit = rateUnit;
    }

    protected ProfileEntry() {
        // for ORM
    }

    public static ProfileEntry climb(final double altitude, final String altitudeUnit,
                                     final double speed, final String speedUnit) {
        return new ProfileEntry(ProfilePhase.CLIMB, altitude, altitudeUnit, speed, speedUnit, null, null);
    }

    public static ProfileEntry climb(final double altitude, final String altitudeUnit,
                                     final double speed, final String speedUnit,
                                     final double rate, final String rateUnit) {
        return new ProfileEntry(ProfilePhase.CLIMB, altitude, altitudeUnit, speed, speedUnit, rate, rateUnit);
    }

    public static ProfileEntry cruise(final double speed, final String speedUnit) {
        return new ProfileEntry(ProfilePhase.CRUISE, null, null, speed, speedUnit, null, null);
    }

    public static ProfileEntry descend(final double altitude, final String altitudeUnit,
                                       final double speed, final String speedUnit,
                                       final double rate, final String rateUnit) {
        return new ProfileEntry(ProfilePhase.DESCEND, altitude, altitudeUnit, speed, speedUnit, rate, rateUnit);
    }

    public ProfilePhase phase() { return phase; }
    public Double altitude() { return altitude; }
    public String altitudeUnit() { return altitudeUnit; }
    public double speed() { return speed; }
    public String speedUnit() { return speedUnit; }
    public Double rate() { return rate; }
    public String rateUnit() { return rateUnit; }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }
}
