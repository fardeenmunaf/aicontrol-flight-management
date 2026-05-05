package flightPlan.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

@Embeddable
public class FlightPlanId implements ValueObject, Comparable<FlightPlanId> {

    private static final long serialVersionUID = 1L;

    private String id;

    public FlightPlanId(final String id) {
        Preconditions.nonEmpty(id, "FlightPlanId must not be null or empty");
        this.id = id;
    }

    protected FlightPlanId() {
        // for ORM
    }

    public static FlightPlanId newUnique() {
        return new FlightPlanId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final FlightPlanId other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightPlanId)) return false;
        return this.id.equals(((FlightPlanId) o).id);
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