package shared.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class EngineModelId implements ValueObject, Comparable<EngineModelId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public EngineModelId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Engine model id should neither be null nor empty");
        }
        this.value = value;
    }

    protected EngineModelId() {
        // for ORM
    }

    public static EngineModelId valueOf(final String value) {
        return new EngineModelId(value);
    }

    public static EngineModelId newId() {
        return new EngineModelId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final EngineModelId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EngineModelId)) {
            return false;
        }

        final EngineModelId other = (EngineModelId) o;
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