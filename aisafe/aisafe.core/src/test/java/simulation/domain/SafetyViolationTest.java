package simulation.domain;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import airtransportcompany.domain.AirTransportCompany;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SafetyViolationTest {

    private static final Instant START = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant END   = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant TS    = Instant.parse("2026-01-01T09:00:00Z");
    private static final aircontrolarea.domain.AirControlArea AREA = null; // area is optional; null allowed

    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737-Sv", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56-SV", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
            new EngineModelMaker("CFM", "France"),
            new FuelProperties(FuelType.JET_A, 0.80, 43.0));

    static { MODEL.addSupportedEngineModel(ENGINE); }

    private static final AirTransportCompany COMPANY =
            new AirTransportCompany("SV Airlines", "SV", "SVL");

    private static final CabinConfiguration CABIN = new CabinConfiguration(150, 10, 20);

    private Simulation simulation() {
        return new Simulation(START, END, SimulationStatus.RUNNING);
    }

    private Set<VelocityVector> twoVectors() {
        return Set.of(new VelocityVector(100.0, 90.0), new VelocityVector(200.0, 180.0));
    }

    private Set<Aircraft> twoAircraft() {
        return Set.of(
                new Aircraft("CS-SV1", "Portugal", AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY),
                new Aircraft("CS-SV2", "Spain",    AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY)
        );
    }

    private Aircraft anAircraft() {
        return new Aircraft("CS-SV1", "Portugal", AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY);
    }

    // --- Construction: happy path ---

    @Test
    void ensureSafetyViolationIsCreatedWithExpectedState() {
        final Simulation sim = simulation();
        final Set<VelocityVector> vectors = twoVectors();
        final Set<Aircraft> aircraft = twoAircraft();

        final SafetyViolation sv = new SafetyViolation(sim, TS, ViolationType.COLLISION, AREA, vectors, aircraft);
        assertAll(
                () -> assertEquals(sim, sv.simulation()),
                () -> assertEquals(TS, sv.timestamp()),
                () -> assertEquals(ViolationType.COLLISION, sv.violationType()),
                () -> assertEquals(2, sv.recordedVectors().size()),
                () -> assertEquals(2, sv.involvedAircraft().size())
        );
    }

    // --- Relaxed invariants ---

    @Test
    void ensureViolationWithNoVectorsOrAircraftIsAccepted() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.FUEL_EXHAUSTION, AREA);
        assertAll(
                () -> assertEquals(0, sv.recordedVectors().size()),
                () -> assertEquals(0, sv.involvedAircraft().size()),
                () -> assertEquals(AREA, sv.airControlArea()),
                () -> assertEquals(ViolationType.FUEL_EXHAUSTION, sv.violationType()));
    }

    @Test
    void ensureSingleAircraftViolationIsAccepted() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA,
                Set.of(new VelocityVector(250.0, 90.0)), Set.of(anAircraft()));
        assertEquals(1, sv.involvedAircraft().size());
    }

    @Test
    void ensureCallsignsConvenienceConstructorStoresCallsigns() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA,
                Set.of("TAP0623A", "AFR0702A"));
        assertEquals(Set.of("TAP0623A", "AFR0702A"), sv.involvedCallsigns());
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullSimulationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(null, TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft()));
    }

    @Test
    void ensureNullTimestampIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), null, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft()));
    }

    @Test
    void ensureNullViolationTypeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), TS, null, AREA, twoVectors(), twoAircraft()));
    }

    // --- Defensive copies ---

    @Test
    void ensureRecordedVectorsIsDefensiveCopy() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        final Set<VelocityVector> vectors = sv.recordedVectors();
        vectors.clear();
        assertEquals(2, sv.recordedVectors().size());
    }

    // --- equals / hashCode ---

    @Test
    void ensureSafetyViolationIsEqualToItself() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        assertEquals(sv, sv);
    }

    @Test
    void ensureSafetyViolationIsNotEqualToNull() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        assertNotEquals(sv, null);
    }

    @Test
    void ensureSafetyViolationHashCodeIsConsistent() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        assertEquals(sv.hashCode(), sv.hashCode());
    }

    @Test
    void ensureSafetyViolationIdentityIsNullBeforePersistence() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        assertNull(sv.identity());
    }

    @Test
    void ensureSafetyViolationSameAsItself() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        assertTrue(sv.sameAs(sv));
    }

    @Test
    void ensureSafetyViolationSameAsNullIsFalse() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        assertFalse(sv.sameAs(null));
    }

    @Test
    void ensureInvolvedAircraftIsDefensiveCopy() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), twoAircraft());
        final Set<Aircraft> aircraft = sv.involvedAircraft();
        aircraft.clear();
        assertEquals(2, sv.involvedAircraft().size());
    }

    @Test
    void ensureNullVectorsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, null, twoAircraft()));
    }

    @Test
    void ensureNullAircraftIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), TS, ViolationType.COLLISION, AREA, twoVectors(), null));
    }
}
