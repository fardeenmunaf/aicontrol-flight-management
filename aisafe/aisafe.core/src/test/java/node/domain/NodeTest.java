package node.domain;

import org.junit.jupiter.api.Test;
import shared.domain.NodeId;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    private static final NodeId NODE_ID = NodeId.newId();
    private static final Coordinates COORDS = new Coordinates(38.7169, -9.1399);

    private Node validNode() {
        return new Node(NODE_ID, "LIS-VOR", COORDS);
    }

    // --- Construction: happy path ---

    @Test
    void ensureNodeIsCreatedWithExpectedState() {
        final Node node = validNode();
        assertAll(
                () -> assertEquals(NODE_ID, node.identity()),
                () -> assertEquals("LIS-VOR", node.designation()),
                () -> assertEquals(COORDS, node.coordinates())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Node(null, "LIS-VOR", COORDS));
    }

    @Test
    void ensureNullDesignationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Node(NODE_ID, null, COORDS));
    }

    @Test
    void ensureEmptyDesignationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Node(NODE_ID, "", COORDS));
    }

    @Test
    void ensureBlankDesignationBehavior() {
        // eapli's StringPredicates.isNullOrEmpty does not treat whitespace-only as empty
        // so whitespace is accepted at domain level; the guard rejects null and ""
        assertDoesNotThrow(() -> new Node(NODE_ID, "   ", COORDS));
    }

    @Test
    void ensureNullCoordinatesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Node(NODE_ID, "LIS-VOR", null));
    }

    // --- equals / hashCode ---

    @Test
    void ensureNodeIsEqualToItself() {
        final Node node = validNode();
        assertEquals(node, node);
        assertEquals(node.hashCode(), node.hashCode());
    }

    @Test
    void ensureNodeIsNotEqualToNull() {
        assertNotEquals(validNode(), null);
    }

    @Test
    void ensureTwoDifferentNodesAreNotEqual() {
        final Node n1 = validNode();
        final Node n2 = new Node(NodeId.newId(), "OPO-VOR", new Coordinates(41.2358, -8.6703));
        assertNotEquals(n1, n2);
    }

    // --- toString ---

    @Test
    void ensureToStringContainsDesignation() {
        final Node node = validNode();
        assertTrue(node.toString().contains("LIS-VOR"));
    }

    @Test
    void ensureNodeSameAsItself() {
        final Node node = validNode();
        assertTrue(node.sameAs(node));
    }

    @Test
    void ensureNodeSameAsNullIsFalse() {
        final Node node = validNode();
        assertFalse(node.sameAs(null));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final Node node = new Node() { };
        assertNotNull(node);
    }
}
