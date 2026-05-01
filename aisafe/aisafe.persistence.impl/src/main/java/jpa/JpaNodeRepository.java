package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import node.domain.Node;
import node.repository.NodeRepository;
import shared.domain.NodeId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of NodeRepository.
 */
class JpaNodeRepository extends BaseJpaRepositoryBase<Node, NodeId, NodeId>
        implements NodeRepository {

    public JpaNodeRepository(final TransactionalContext autoTx) {
        super(autoTx, "nodeId");
    }

    public JpaNodeRepository(final String persistenceUnitName) {
        super("nodeId");
    }

    @Override
    public Optional<Node> findByDesignation(final String designation) {
        final Map<String, Object> params = new HashMap<>();
        params.put("designation", designation);
        return matchOne("e.designation = :designation", params);
    }
}

