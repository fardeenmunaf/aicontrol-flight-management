package shared.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class SimulationId implements ValueObject, Comparable<SimulationId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public SimulationId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Simulation id should neither be null nor empty");
        }
        this.value = value;
    }

    protected SimulationId() {
        // for ORM
    }

    public static SimulationId valueOf(final String value) {
        return new SimulationId(value);
    }

    public static SimulationId newId() {
        return new SimulationId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final SimulationId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimulationId)) {
            return false;
        }

        final SimulationId other = (SimulationId) o;
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