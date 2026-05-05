package shared.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class SimulationReportId implements ValueObject, Comparable<SimulationReportId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public SimulationReportId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Simulation report id should neither be null nor empty");
        }
        this.value = value;
    }

    protected SimulationReportId() {
        // for ORM
    }

    public static SimulationReportId valueOf(final String value) {
        return new SimulationReportId(value);
    }

    public static SimulationReportId newId() {
        return new SimulationReportId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final SimulationReportId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimulationReportId)) {
            return false;
        }

        final SimulationReportId other = (SimulationReportId) o;
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