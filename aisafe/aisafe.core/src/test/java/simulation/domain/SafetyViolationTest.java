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
    private static final Instant END = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant TS = Instant.parse("2026-01-01T09:00:00Z");

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
                new Aircraft("CS-SV2", "Spain", AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY)
        );
    }

    // --- Construction: happy path ---

    @Test
    void ensureSafetyViolationIsCreatedWithExpectedState() {
        final Simulation sim = simulation();
        final Set<VelocityVector> vectors = twoVectors();
        final Set<Aircraft> aircraft = twoAircraft();

        final SafetyViolation sv = new SafetyViolation(sim, TS, "HIGH", vectors, aircraft);
        assertAll(
                () -> assertEquals(sim, sv.simulation()),
                () -> assertEquals(TS, sv.timestamp()),
                () -> assertEquals("HIGH", sv.severity()),
                () -> assertEquals(2, sv.recordedVectors().size()),
                () -> assertEquals(2, sv.involvedAircraft().size())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullSimulationIsRejected() {
        assertThrows(Exception.class,
                () -> new SafetyViolation(null, TS, "HIGH", twoVectors(), twoAircraft()));
    }

    @Test
    void ensureNullTimestampIsRejected() {
        assertThrows(Exception.class,
                () -> new SafetyViolation(simulation(), null, "HIGH", twoVectors(), twoAircraft()));
    }

    @Test
    void ensureNullSeverityIsRejected() {
        assertThrows(Exception.class,
                () -> new SafetyViolation(simulation(), TS, null, twoVectors(), twoAircraft()));
    }

    @Test
    void ensureSingleVectorIsRejected() {
        final Set<VelocityVector> oneVector = Set.of(new VelocityVector(100.0, 90.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), TS, "HIGH", oneVector, twoAircraft()));
    }

    @Test
    void ensureEmptyVectorsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), TS, "HIGH", Set.of(), twoAircraft()));
    }

    @Test
    void ensureSingleAircraftIsRejected() {
        final Set<Aircraft> oneAircraft = Set.of(
                new Aircraft("CS-ONE", "Portugal", AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY));
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), oneAircraft));
    }

    // --- Defensive copies ---

    @Test
    void ensureRecordedVectorsIsDefensiveCopy() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        final Set<VelocityVector> vectors = sv.recordedVectors();
        vectors.clear();
        assertEquals(2, sv.recordedVectors().size());
    }

    // --- equals / hashCode ---

    @Test
    void ensureSafetyViolationIsEqualToItself() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        assertEquals(sv, sv);
    }

    @Test
    void ensureSafetyViolationIsNotEqualToNull() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        assertNotEquals(sv, null);
    }

    @Test
    void ensureSafetyViolationHashCodeIsConsistent() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        assertEquals(sv.hashCode(), sv.hashCode());
    }

    @Test
    void ensureSafetyViolationIdentityIsNullBeforePersistence() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        assertNull(sv.identity());
    }

    @Test
    void ensureSafetyViolationSameAsItself() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        assertTrue(sv.sameAs(sv));
    }

    @Test
    void ensureSafetyViolationSameAsNullIsFalse() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        assertFalse(sv.sameAs(null));
    }

    @Test
    void ensureInvolvedAircraftIsDefensiveCopy() {
        final SafetyViolation sv = new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), twoAircraft());
        final Set<Aircraft> aircraft = sv.involvedAircraft();
        aircraft.clear();
        assertEquals(2, sv.involvedAircraft().size());
    }

    @Test
    void ensureNullVectorsIsRejected() {
        assertThrows(Exception.class,
                () -> new SafetyViolation(simulation(), TS, "HIGH", null, twoAircraft()));
    }

    @Test
    void ensureNullAircraftIsRejected() {
        assertThrows(Exception.class,
                () -> new SafetyViolation(simulation(), TS, "HIGH", twoVectors(), null));
    }
}
