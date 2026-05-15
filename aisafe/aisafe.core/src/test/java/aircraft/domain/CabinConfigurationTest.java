package aircraft.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CabinConfigurationTest {

    // --- Construction: happy path ---

    @Test
    void ensureCabinConfigurationIsCreatedWithExpectedState() {
        final CabinConfiguration cabin = new CabinConfiguration(150, 10, 20);
        assertAll(
                () -> assertEquals(150, cabin.economyClassSeats()),
                () -> assertEquals(10, cabin.firstClassSeats()),
                () -> assertEquals(20, cabin.businessClassSeats())
        );
    }

    @Test
    void ensureCabinConfigurationWithZeroSeatsIsAccepted() {
        assertDoesNotThrow(() -> new CabinConfiguration(0, 0, 0));
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNegativeEconomySeatsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CabinConfiguration(-1, 10, 20));
    }

    @Test
    void ensureNegativeFirstClassSeatsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CabinConfiguration(150, -1, 20));
    }

    @Test
    void ensureNegativeBusinessClassSeatsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CabinConfiguration(150, 10, -1));
    }

    // --- equals / hashCode ---

    @Test
    void ensureEqualCabinConfigurationsAreEqual() {
        final CabinConfiguration c1 = new CabinConfiguration(150, 10, 20);
        final CabinConfiguration c2 = new CabinConfiguration(150, 10, 20);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void ensureDifferentCabinConfigurationsAreNotEqual() {
        final CabinConfiguration c1 = new CabinConfiguration(150, 10, 20);
        final CabinConfiguration c2 = new CabinConfiguration(100, 5, 15);
        assertNotEquals(c1, c2);
    }

    @Test
    void ensureCabinConfigurationIsEqualToItself() {
        final CabinConfiguration c = new CabinConfiguration(150, 10, 20);
        assertEquals(c, c);
    }

    @Test
    void ensureCabinConfigurationIsNotEqualToNull() {
        assertNotEquals(new CabinConfiguration(150, 10, 20), null);
    }

    @Test
    void ensureCabinConfigurationIsNotEqualToObjectOfDifferentType() {
        final CabinConfiguration c = new CabinConfiguration(150, 10, 20);
        assertNotEquals(c, "some string");
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final CabinConfiguration c = new CabinConfiguration() { };
        assertNotNull(c);
    }
}
