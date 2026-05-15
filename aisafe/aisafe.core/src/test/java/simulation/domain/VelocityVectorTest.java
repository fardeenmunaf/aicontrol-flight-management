package simulation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VelocityVectorTest {

    // --- Construction: happy path ---

    @Test
    void ensureVelocityVectorIsCreatedWithExpectedState() {
        final VelocityVector vv = new VelocityVector(250.0, 90.0);
        assertAll(
                () -> assertEquals(250.0, vv.speed()),
                () -> assertEquals(90.0, vv.direction())
        );
    }

    @Test
    void ensureZeroSpeedIsAccepted() {
        assertDoesNotThrow(() -> new VelocityVector(0.0, 90.0));
    }

    @Test
    void ensureZeroDirectionIsAccepted() {
        assertDoesNotThrow(() -> new VelocityVector(100.0, 0.0));
    }

    @Test
    void ensure360DirectionIsAccepted() {
        assertDoesNotThrow(() -> new VelocityVector(100.0, 360.0));
    }

    @Test
    void ensureValueOfFactoryMethodWorks() {
        final VelocityVector vv = VelocityVector.valueOf(100.0, 180.0);
        assertEquals(100.0, vv.speed());
        assertEquals(180.0, vv.direction());
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VelocityVector(null, 90.0));
    }

    @Test
    void ensureNullDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VelocityVector(100.0, null));
    }

    @Test
    void ensureNegativeSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VelocityVector(-1.0, 90.0));
    }

    @Test
    void ensureNegativeDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VelocityVector(100.0, -1.0));
    }

    @Test
    void ensureDirectionGreaterThan360IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VelocityVector(100.0, 361.0));
    }

    // --- equals / hashCode ---

    @Test
    void ensureEqualVelocityVectorsAreEqual() {
        final VelocityVector vv1 = new VelocityVector(250.0, 90.0);
        final VelocityVector vv2 = new VelocityVector(250.0, 90.0);
        assertEquals(vv1, vv2);
        assertEquals(vv1.hashCode(), vv2.hashCode());
    }

    @Test
    void ensureDifferentVelocityVectorsAreNotEqual() {
        final VelocityVector vv1 = new VelocityVector(250.0, 90.0);
        final VelocityVector vv2 = new VelocityVector(300.0, 180.0);
        assertNotEquals(vv1, vv2);
    }

    @Test
    void ensureVelocityVectorIsNotEqualToNull() {
        assertNotEquals(new VelocityVector(100.0, 90.0), null);
    }

    @Test
    void ensureVelocityVectorIsEqualToItself() {
        final VelocityVector vv = new VelocityVector(250.0, 90.0);
        assertEquals(vv, vv);
    }

    // --- toString ---

    @Test
    void ensureToStringContainsSpeedAndDirection() {
        final VelocityVector vv = new VelocityVector(250.0, 90.0);
        final String str = vv.toString();
        assertTrue(str.contains("250.0"));
        assertTrue(str.contains("90.0"));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final VelocityVector vv = new VelocityVector() { };
        assertNotNull(vv);
    }
}
