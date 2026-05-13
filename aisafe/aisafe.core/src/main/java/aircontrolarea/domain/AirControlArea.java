package aircontrolarea.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import shared.domain.AirControlAreaId;

@Entity
public class AirControlArea implements AggregateRoot<AirControlAreaId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private AirControlAreaId airControlAreaId;

    private String areaCode;
    private double minimumRequiredArrivalFuel;
    private String geographicBoundaries;

    public AirControlArea(final String areaCode,
                          final double minimumRequiredArrivalFuel,
                          final String geographicBoundaries) {

        Preconditions.noneNull(areaCode, geographicBoundaries);
        if (StringPredicates.isNullOrEmpty(areaCode)) {
            throw new IllegalArgumentException("Area code should neither be null nor empty");
        }
        if (minimumRequiredArrivalFuel < 0) {
            throw new IllegalArgumentException("Minimum required arrival fuel cannot be negative");
        }
        if (StringPredicates.isNullOrEmpty(geographicBoundaries)) {
            throw new IllegalArgumentException("Geographic boundaries should neither be null nor empty");
        }

        this.areaCode = areaCode;
        this.minimumRequiredArrivalFuel = minimumRequiredArrivalFuel;
        this.geographicBoundaries = geographicBoundaries;
        this.airControlAreaId = AirControlAreaId.newId();
    }

    protected AirControlArea() {
        // for ORM
    }

    @Override
    public AirControlAreaId identity() {
        return airControlAreaId;
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

    public String areaCode() {
        return areaCode;
    }

    public double minimumRequiredArrivalFuel() {
        return minimumRequiredArrivalFuel;
    }

    public String geographicBoundaries() {
        return geographicBoundaries;
    }

    /**
     * Returns true if the given point (latitude, longitude) falls inside this area's polygon.
     * The geographicBoundaries string must follow the format "lat,lon;lat,lon;...".
     * Returns false if the format is invalid or unparseable.
     */
    public boolean containsPoint(final double latitude, final double longitude) {
        try {
            final String[] tokens = geographicBoundaries.split(";");
            final int n = tokens.length;
            if (n < 3) {
                return false;
            }
            final double[] lats = new double[n];
            final double[] lons = new double[n];
            for (int k = 0; k < n; k++) {
                final String[] parts = tokens[k].trim().split(",");
                lats[k] = Double.parseDouble(parts[0].trim());
                lons[k] = Double.parseDouble(parts[1].trim());
            }
            // Ray casting: horizontal ray from (latitude, longitude) going right
            boolean inside = false;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                final double yi = lats[i], xi = lons[i];
                final double yj = lats[j], xj = lons[j];
                if (((yi > latitude) != (yj > latitude))
                        && (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi)) {
                    inside = !inside;
                }
            }
            return inside;
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Code: %s | MinFuel: %.2f | Boundaries: %s",
                areaCode,
                minimumRequiredArrivalFuel,
                geographicBoundaries
        );
    }

}

