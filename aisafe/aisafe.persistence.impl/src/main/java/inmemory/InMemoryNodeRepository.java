package inmemory;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import node.domain.Node;
import node.repository.NodeRepository;
import shared.domain.NodeId;

import java.util.Optional;

/**
 * In-memory implementation of NodeRepository.
 */
public class InMemoryNodeRepository extends InMemoryDomainRepository<Node, NodeId>
        implements NodeRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<Node> findByDesignation(final String designation) {
        return matchOne(n -> n.designation().equals(designation));
    }
}

