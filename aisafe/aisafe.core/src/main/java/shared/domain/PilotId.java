package shared.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class PilotId implements ValueObject, Comparable<PilotId> {

    private static final long serialVersionUID = 1L;

    @Column(name = "pilot_id")

    private String value;

    public PilotId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Pilot id should neither be null nor empty");
        }
        this.value = value;
    }

    protected PilotId() {
        // for ORM
    }

    public static PilotId valueOf(final String value) {
        return new PilotId(value);
    }

    public static PilotId newId() {
        return new PilotId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final PilotId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PilotId)) {
            return false;
        }

        final PilotId other = (PilotId) o;
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
