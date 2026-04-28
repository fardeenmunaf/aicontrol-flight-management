package shared.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class FlightId implements ValueObject, Comparable<FlightId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public FlightId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Flight id should neither be null nor empty");
        }
        this.value = value;
    }

    protected FlightId() {
        // for ORM
    }

    public static FlightId valueOf(final String value) {
        return new FlightId(value);
    }

    @Override
    public int compareTo(final FlightId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlightId)) {
            return false;
        }

        final FlightId other = (FlightId) o;
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }
}
