package shared.domain;

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

