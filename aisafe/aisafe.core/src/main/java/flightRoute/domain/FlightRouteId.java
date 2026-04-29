package flightRoute.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

@Embeddable
public class FlightRouteId implements ValueObject, Serializable, Comparable<FlightRouteId> {

    private static final long serialVersionUID = 1L;

    private String id;

    public FlightRouteId(final String id) {
        Preconditions.nonEmpty(id, "FlightRouteId must not be null or empty");
        this.id = id;
    }

    protected FlightRouteId() {
        // for ORM
    }

    public static FlightRouteId newUnique() {
        return new FlightRouteId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final FlightRouteId other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightRouteId)) return false;
        return this.id.equals(((FlightRouteId) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}