package node.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;
import org.yaml.snakeyaml.nodes.NodeId;

@Entity
public class Node implements AggregateRoot<NodeId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private NodeId nodeId;

    private String designation;

    private Coordinates coordinates;

    public Node(final NodeId nodeId, final String designation, final Coordinates coordinates) {
        Preconditions.noneNull(nodeId, coordinates);
        if (StringPredicates.isNullOrEmpty(designation)) {
            throw new IllegalArgumentException("Node designation must not be null or empty");
        }
        this.nodeId      = nodeId;
        this.designation = designation;
        this.coordinates = coordinates;
    }

    protected Node() {
        // for ORM
    }

    @Override
    public NodeId identity() {
        return nodeId;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    public String designation() {
        return designation;
    }

    public Coordinates coordinates() {
        return coordinates;
    }

    @Override
    public String toString() {
        return String.format("Node{id=%s, designation='%s', coordinates=%s}",
                nodeId, designation, coordinates);
    }
}