package node.domain;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

@Embeddable
public class Coordinates implements ValueObject, Serializable {

    private static final long serialVersionUID = 1L;

    private static final double MIN_LATITUDE  = -90.0;
    private static final double MAX_LATITUDE  =  90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE =  180.0;

    private double latitude;
    private double longitude;

    public Coordinates(final double latitude, final double longitude) {
        Preconditions.ensure(
                latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE,
                "Latitude must be between -90 and 90 degrees"
        );
        Preconditions.ensure(
                longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE,
                "Longitude must be between -180 and 180 degrees"
        );
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    protected Coordinates() {
        // for ORM
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates)) return false;
        final Coordinates other = (Coordinates) o;
        return Double.compare(this.latitude, other.latitude)  == 0
                && Double.compare(this.longitude, other.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(latitude) + Double.hashCode(longitude);
    }

    @Override
    public String toString() {
        return String.format("(%.6f, %.6f)", latitude, longitude);
    }
}