package enginemodel.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuelPropertiesTest {

    @Test
    void ensureFuelPropertiesIsCreatedWithExpectedState() {
        final FuelProperties props = new FuelProperties(FuelType.JET_A, 0.8, 43.2);
        assertAll(
                () -> assertEquals(FuelType.JET_A, props.type()),
                () -> assertEquals(0.8, props.density()),
                () -> assertEquals(43.2, props.specificEnergy())
        );
    }

    @Test
    void ensureNullTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(null, 0.8, 43.2));
    }

    @Test
    void ensureNullDensityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(FuelType.JET_A, null, 43.2));
    }

    @Test
    void ensureNullSpecificEnergyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(FuelType.JET_A, 0.8, null));
    }

    @Test
    void ensureZeroDensityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(FuelType.JET_A, 0.0, 43.2));
    }

    @Test
    void ensureNegativeDensityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(FuelType.JET_A, -0.1, 43.2));
    }

    @Test
    void ensureZeroSpecificEnergyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(FuelType.JET_A, 0.8, 0.0));
    }

    @Test
    void ensureNegativeSpecificEnergyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FuelProperties(FuelType.JET_A, 0.8, -1.0));
    }

    @Test
    void ensureDifferentFuelTypesAreAccepted() {
        assertDoesNotThrow(() -> new FuelProperties(FuelType.AVGAS, 0.72, 42.0));
        assertDoesNotThrow(() -> new FuelProperties(FuelType.DIESEL, 0.84, 42.5));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final FuelProperties props = new FuelProperties() { };
        assertNotNull(props);
    }
}
