package flightPlan.domain;

import org.junit.jupiter.api.Test;
import shared.domain.NodeId;

import static org.junit.jupiter.api.Assertions.*;

class SegmentTest {

    private static final NodeId NODE_A = NodeId.newId();
    private static final NodeId NODE_B = NodeId.newId();

    private Segment validSegment() {
        return new Segment(NODE_A, NODE_B, 90.0, 5.0);
    }

    // --- Construction: happy path ---

    @Test
    void ensureSegmentIsCreatedWithExpectedState() {
        final Segment segment = validSegment();
        assertAll(
                () -> assertEquals(NODE_A, segment.startNodeId()),
                () -> assertEquals(NODE_B, segment.endNodeId()),
                () -> assertEquals(90.0, segment.windDirection()),
                () -> assertEquals(5.0, segment.windSpeed()),
                () -> assertTrue(segment.altitudeSlots().isEmpty())
        );
    }

    @Test
    void ensureZeroWindSpeedIsAccepted() {
        assertDoesNotThrow(() -> new Segment(NODE_A, NODE_B, 0.0, 0.0));
    }

    @Test
    void ensureWindDirectionAtUpperBoundMinusOneIsAccepted() {
        assertDoesNotThrow(() -> new Segment(NODE_A, NODE_B, 359.9, 5.0));
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullStartNodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Segment(null, NODE_B, 90.0, 5.0));
    }

    @Test
    void ensureNullEndNodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Segment(NODE_A, null, 90.0, 5.0));
    }

    @Test
    void ensureSameStartAndEndNodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Segment(NODE_A, NODE_A, 90.0, 5.0));
    }

    @Test
    void ensureNegativeWindDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Segment(NODE_A, NODE_B, -1.0, 5.0));
    }

    @Test
    void ensureWindDirectionAt360IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Segment(NODE_A, NODE_B, 360.0, 5.0));
    }

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Segment(NODE_A, NODE_B, 90.0, -1.0));
    }

    // --- addAltitudeSlot ---

    @Test
    void ensureAltitudeSlotCanBeAdded() {
        final Segment segment = validSegment();
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        segment.addAltitudeSlot(slot);
        assertEquals(1, segment.altitudeSlots().size());
    }

    @Test
    void ensureNullAltitudeSlotIsRejected() {
        final Segment segment = validSegment();
        assertThrows(IllegalArgumentException.class, () -> segment.addAltitudeSlot(null));
    }

    // --- equals / hashCode ---

    @Test
    void ensureSegmentIsEqualToItself() {
        final Segment segment = validSegment();
        assertEquals(segment, segment);
    }

    @Test
    void ensureSegmentIsNotEqualToNull() {
        assertNotEquals(validSegment(), null);
    }

    @Test
    void ensureSegmentHashCodeIsConsistent() {
        final Segment segment = validSegment();
        assertEquals(segment.hashCode(), segment.hashCode());
    }

    @Test
    void ensureSegmentIdentityIsNullBeforePersistence() {
        final Segment segment = validSegment();
        assertNull(segment.identity());
    }

    @Test
    void ensureSegmentSameAsItself() {
        final Segment segment = validSegment();
        assertTrue(segment.sameAs(segment));
    }

    @Test
    void ensureSegmentSameAsNullIsFalse() {
        final Segment segment = validSegment();
        assertFalse(segment.sameAs(null));
    }
}
