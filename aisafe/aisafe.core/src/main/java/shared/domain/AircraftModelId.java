package shared.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AircraftModelId implements ValueObject, Comparable<AircraftModelId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public AircraftModelId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Aircraft model id should neither be null nor empty");
        }
        this.value = value;
    }

    protected AircraftModelId() {
        // for ORM
    }

    public static AircraftModelId valueOf(final String value) {
        return new AircraftModelId(value);
    }

    @Override
    public int compareTo(final AircraftModelId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AircraftModelId)) {
            return false;
        }

        final AircraftModelId other = (AircraftModelId) o;
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
