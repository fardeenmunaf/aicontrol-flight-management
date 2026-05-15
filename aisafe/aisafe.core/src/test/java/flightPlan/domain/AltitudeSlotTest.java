package flightPlan.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AltitudeSlotTest {

    @Test
    void ensureAltitudeSlotIsCreatedWithExpectedState() {
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        assertAll(
                () -> assertEquals(1000.0, slot.minimumAltitude()),
                () -> assertEquals(5000.0, slot.maximumAltitude()),
                () -> assertEquals(100.0, slot.width())
        );
    }

    @Test
    void ensureZeroMinimumAltitudeIsAccepted() {
        assertDoesNotThrow(() -> new AltitudeSlot(0.0, 1000.0, 50.0));
    }

    @Test
    void ensureNegativeMinimumAltitudeIsRejected() {
        assertThrows(Exception.class, () -> new AltitudeSlot(-1.0, 1000.0, 50.0));
    }

    @Test
    void ensureMaximumAltitudeLessThanMinimumIsRejected() {
        assertThrows(Exception.class, () -> new AltitudeSlot(5000.0, 1000.0, 50.0));
    }

    @Test
    void ensureMaximumAltitudeEqualToMinimumIsRejected() {
        assertThrows(Exception.class, () -> new AltitudeSlot(1000.0, 1000.0, 50.0));
    }

    @Test
    void ensureZeroWidthIsRejected() {
        assertThrows(Exception.class, () -> new AltitudeSlot(0.0, 1000.0, 0.0));
    }

    @Test
    void ensureNegativeWidthIsRejected() {
        assertThrows(Exception.class, () -> new AltitudeSlot(0.0, 1000.0, -1.0));
    }

    @Test
    void ensureAltitudeSlotIsEqualToItself() {
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        assertEquals(slot, slot);
    }

    @Test
    void ensureAltitudeSlotIsNotEqualToNull() {
        assertNotEquals(new AltitudeSlot(1000.0, 5000.0, 100.0), null);
    }

    @Test
    void ensureAltitudeSlotHashCodeIsConsistent() {
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        assertEquals(slot.hashCode(), slot.hashCode());
    }

    @Test
    void ensureAltitudeSlotIdentityIsNullBeforePersistence() {
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        // identity() returns null until persisted (JPA-generated ID)
        assertNull(slot.identity());
    }

    @Test
    void ensureAltitudeSlotSameAsItself() {
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        assertTrue(slot.sameAs(slot));
    }

    @Test
    void ensureAltitudeSlotSameAsNullIsFalse() {
        final AltitudeSlot slot = new AltitudeSlot(1000.0, 5000.0, 100.0);
        assertFalse(slot.sameAs(null));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final AltitudeSlot slot = new AltitudeSlot() { };
        assertNotNull(slot);
    }
}
