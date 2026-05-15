package pilot.domain;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import org.junit.jupiter.api.Test;
import shared.domain.UserId;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PilotTest {

    private static final UserId USER_ID = UserId.valueOf("pilot@test.com");

    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737-Pilot", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private Pilot validPilot() {
        return new Pilot(USER_ID, PilotStatus.ACTIVE, Set.of(MODEL));
    }

    // --- Construction: happy path ---

    @Test
    void ensurePilotIsCreatedWithExpectedState() {
        final Pilot pilot = validPilot();
        assertAll(
                () -> assertEquals(USER_ID, pilot.userId()),
                () -> assertEquals(PilotStatus.ACTIVE, pilot.status()),
                () -> assertEquals(1, pilot.certifiedAircraftModels().size()),
                () -> assertNotNull(pilot.identity())
        );
    }

    @Test
    void ensurePilotWithEmptyCertificationsIsCreated() {
        final Pilot pilot = new Pilot(USER_ID, PilotStatus.ACTIVE, new HashSet<>());
        assertTrue(pilot.certifiedAircraftModels().isEmpty());
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullUserIdIsRejected() {
        assertThrows(Exception.class, () -> new Pilot(null, PilotStatus.ACTIVE, Set.of()));
    }

    @Test
    void ensureNullStatusIsRejected() {
        assertThrows(Exception.class, () -> new Pilot(USER_ID, null, Set.of()));
    }

    @Test
    void ensureNullCertificationsIsRejected() {
        assertThrows(Exception.class, () -> new Pilot(USER_ID, PilotStatus.ACTIVE, null));
    }

    // --- certifiedAircraftModels defensive copy ---

    @Test
    void ensureCertifiedModelsIsDefensiveCopy() {
        final Pilot pilot = validPilot();
        final Set<AircraftModel> models = pilot.certifiedAircraftModels();
        models.clear();
        assertEquals(1, pilot.certifiedAircraftModels().size());
    }

    // --- equals / hashCode ---

    @Test
    void ensurePilotIsEqualToItself() {
        final Pilot pilot = validPilot();
        assertEquals(pilot, pilot);
        assertEquals(pilot.hashCode(), pilot.hashCode());
    }

    @Test
    void ensurePilotIsNotEqualToNull() {
        assertNotEquals(validPilot(), null);
    }

    @Test
    void ensureTwoDifferentPilotsAreNotEqual() {
        final Pilot p1 = validPilot();
        final Pilot p2 = new Pilot(UserId.valueOf("other@test.com"), PilotStatus.ACTIVE, Set.of());
        assertNotEquals(p1, p2);
    }

    @Test
    void ensurePilotSameAsItself() {
        final Pilot pilot = validPilot();
        assertTrue(pilot.sameAs(pilot));
    }

    @Test
    void ensurePilotSameAsNullIsFalse() {
        final Pilot pilot = validPilot();
        assertFalse(pilot.sameAs(null));
    }

    @Test
    void ensureProtectedOrmConstructorIsExercised() {
        // Exercise the protected ORM no-arg constructor via anonymous subclass
        final Pilot pilot = new Pilot() { };
        assertNotNull(pilot);
    }
}
