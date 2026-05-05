package shared.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AircraftId implements ValueObject, Comparable<AircraftId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public AircraftId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Aircraft id should neither be null nor empty");
        }
        this.value = value;
    }

    protected AircraftId() {
        // for ORM
    }

    public static AircraftId valueOf(final String value) {
        return new AircraftId(value);
    }

    public static AircraftId newId() {
        return new AircraftId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final AircraftId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AircraftId)) {
            return false;
        }

        final AircraftId other = (AircraftId) o;
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