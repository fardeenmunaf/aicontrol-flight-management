package shared.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AirportId implements ValueObject, Comparable<AirportId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "airport_id")

    private String value;

    public AirportId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Airport id should neither be null nor empty");
        }
        this.value = value;
    }

    protected AirportId() {
        // for ORM
    }

    public static AirportId valueOf(final String value) {
        return new AirportId(value);
    }

    public static AirportId newId() {
        return new AirportId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final AirportId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AirportId)) {
            return false;
        }

        final AirportId other = (AirportId) o;
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