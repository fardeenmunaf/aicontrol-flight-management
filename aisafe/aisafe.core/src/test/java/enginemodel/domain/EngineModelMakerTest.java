package enginemodel.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineModelMakerTest {

    @Test
    void ensureMakerIsCreatedWithExpectedState() {
        final EngineModelMaker maker = new EngineModelMaker("CFM", "USA");
        assertAll(
                () -> assertEquals("CFM", maker.name()),
                () -> assertEquals("USA", maker.country())
        );
    }

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EngineModelMaker(null, "USA"));
    }

    @Test
    void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EngineModelMaker("", "USA"));
    }

    @Test
    void ensureWhitespaceNameIsAcceptedByLibrary() {
        // The eapli StringPredicates.isNullOrEmpty does not treat whitespace-only as empty
        assertDoesNotThrow(() -> new EngineModelMaker("  ", "USA"));
    }

    @Test
    void ensureNullCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EngineModelMaker("CFM", null));
    }

    @Test
    void ensureEmptyCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EngineModelMaker("CFM", ""));
    }

    @Test
    void ensureWhitespaceCountryIsAcceptedByLibrary() {
        // The eapli StringPredicates.isNullOrEmpty does not treat whitespace-only as empty
        assertDoesNotThrow(() -> new EngineModelMaker("CFM", "  "));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final EngineModelMaker maker = new EngineModelMaker() { };
        assertNotNull(maker);
    }
}
