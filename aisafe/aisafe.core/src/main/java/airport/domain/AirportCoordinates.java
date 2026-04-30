package airport.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

@Embeddable
public class AirportCoordinates implements ValueObject {

    private static final long serialVersionUID = 1L;

    private Double latitude;
    private Double longitude;

    protected AirportCoordinates() {
        // for ORM
    }

    public AirportCoordinates(final Double latitude, final Double longitude) {
        Preconditions.noneNull(latitude, longitude);

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double latitude() {
        return latitude;
    }

    public Double longitude() {
        return longitude;
    }
}