package aircraftmodel.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftModelMakerTest {

    @Test
    void ensureMakerIsCreatedWithExpectedState() {
        final AircraftModelMaker maker = new AircraftModelMaker("Boeing", "USA");
        assertAll(
                () -> assertEquals("Boeing", maker.name()),
                () -> assertEquals("USA", maker.country())
        );
    }

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftModelMaker(null, "USA"));
    }

    @Test
    void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftModelMaker("", "USA"));
    }

    @Test
    void ensureNullCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftModelMaker("Boeing", null));
    }

    @Test
    void ensureEmptyCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AircraftModelMaker("Boeing", ""));
    }

    @Test
    void ensureMakerWithWhitespaceNameIsAcceptedByLibrary() {
        // The eapli StringPredicates.isNullOrEmpty does not treat whitespace-only as empty
        // so this simply ensures no exception is thrown (library behavior)
        assertDoesNotThrow(() -> new AircraftModelMaker("  ", "USA"));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final AircraftModelMaker maker = new AircraftModelMaker() { };
        assertNotNull(maker);
    }
}
