package shared.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AirControlAreaId implements ValueObject, Comparable<AirControlAreaId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public AirControlAreaId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Air control area id should neither be null nor empty");
        }
        this.value = value;
    }

    protected AirControlAreaId() {
        // for ORM
    }

    public static AirControlAreaId valueOf(final String value) {
        return new AirControlAreaId(value);
    }

    public static AirControlAreaId newId() {
        return new AirControlAreaId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final AirControlAreaId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AirControlAreaId)) {
            return false;
        }

        final AirControlAreaId other = (AirControlAreaId) o;
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