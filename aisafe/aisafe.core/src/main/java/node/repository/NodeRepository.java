package node.repository;

import eapli.framework.domain.repositories.DomainRepository;
import node.domain.Node;
import shared.domain.NodeId;

import java.util.Optional;

/**
 * Repository interface for Node aggregate.
 */
public interface NodeRepository extends DomainRepository<NodeId, Node> {

    Optional<Node> findByDesignation(String designation);
}

