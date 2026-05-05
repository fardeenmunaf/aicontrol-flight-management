package shared.domain;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class NodeId implements ValueObject, Comparable<NodeId> {

    private static final long serialVersionUID = 1L;

    private String value;

    public NodeId(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Node id should neither be null nor empty");
        }
        this.value = value;
    }

    protected NodeId() {
        // for ORM
    }

    public static NodeId valueOf(final String value) {
        return new NodeId(value);
    }

    public static NodeId newId() {
        return new NodeId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(final NodeId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeId)) {
            return false;
        }

        final NodeId other = (NodeId) o;
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

