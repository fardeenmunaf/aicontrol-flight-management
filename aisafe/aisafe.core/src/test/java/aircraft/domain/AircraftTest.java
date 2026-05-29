package aircraft.domain;

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

import static org.junit.jupiter.api.Assertions.*;

class AircraftTest {

    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
            new EngineModelMaker("CFM", "France"),
            new FuelProperties(FuelType.JET_A, 0.80, 43.0));

    static {
        MODEL.addSupportedEngineModel(ENGINE);
    }

    private static final AirTransportCompany COMPANY =
            new AirTransportCompany("Test Airlines", "TA", "TAL");

    private static final CabinConfiguration CABIN =
            new CabinConfiguration(150, 10, 20);

    private Aircraft validAircraft() {
        return new Aircraft("CS-TKA", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 5,
                MODEL, ENGINE, CABIN, COMPANY);
    }

    @Test
    void ensureAircraftIsCreatedWithExpectedState() {
        final Aircraft aircraft = validAircraft();
        assertAll(
                () -> assertEquals("Portugal", aircraft.registrationCountry()),
                () -> assertEquals(5, aircraft.flightCrewCount()),
                () -> assertEquals(AircraftOperationStatus.IN_SERVICE, aircraft.operationStatus()),
                () -> assertEquals(MODEL, aircraft.aircraftModel()),
                () -> assertEquals(ENGINE, aircraft.engineModel()),
                () -> assertEquals(CABIN, aircraft.cabinConfiguration()),
                () -> assertEquals(COMPANY, aircraft.airTransportCompany()),
                () -> assertNotNull(aircraft.identity())
        );
    }

    @Test
    void ensureNullRegistrationNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft(null, "Portugal", AircraftOperationStatus.IN_SERVICE, 5, MODEL, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureEmptyRegistrationNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("", "Portugal", AircraftOperationStatus.IN_SERVICE, 5, MODEL, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureNullRegistrationCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", null, AircraftOperationStatus.IN_SERVICE, 5, MODEL, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureEmptyRegistrationCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "", AircraftOperationStatus.IN_SERVICE, 5, MODEL, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureNullOperationStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", null, 5, MODEL, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureNegativeFlightCrewCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", AircraftOperationStatus.IN_SERVICE, -1, MODEL, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureNullAircraftModelIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", AircraftOperationStatus.IN_SERVICE, 5, null, ENGINE, CABIN, COMPANY));
    }

    @Test
    void ensureNullEngineModelIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", AircraftOperationStatus.IN_SERVICE, 5, MODEL, null, CABIN, COMPANY));
    }

    @Test
    void ensureUncertifiedEngineModelIsRejected() {
        final EngineModel other = new EngineModel(
                "Other", EngineType.TURBOPROP, 50.0, 0.30, 50000.0, 40000.0, 0.50,
                new EngineModelMaker("Other", "USA"),
                new FuelProperties(FuelType.AVGAS, 0.72, 44.0));
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", AircraftOperationStatus.IN_SERVICE, 5, MODEL, other, CABIN, COMPANY));
    }

    @Test
    void ensureNullCabinConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", AircraftOperationStatus.IN_SERVICE, 5, MODEL, ENGINE, null, COMPANY));
    }

    @Test
    void ensureNullCompanyIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Aircraft("CS-TKA", "Portugal", AircraftOperationStatus.IN_SERVICE, 5, MODEL, ENGINE, CABIN, null));
    }

    @Test
    void ensureActiveAircraftCanBeDecommissioned() {
        final Aircraft aircraft = validAircraft();
        aircraft.decommission();
        assertEquals(AircraftOperationStatus.DECOMISSIONED, aircraft.operationStatus());
    }

    @Test
    void ensureDecommissioningAlreadyDecommissionedAircraftIsRejected() {
        final Aircraft aircraft = validAircraft();
        aircraft.decommission();
        assertThrows(IllegalStateException.class, aircraft::decommission);
    }

    @Test
    void ensureAircraftIsEqualToItself() {
        final Aircraft aircraft = validAircraft();
        assertEquals(aircraft, aircraft);
        assertEquals(aircraft.hashCode(), aircraft.hashCode());
    }

    @Test
    void ensureAircraftIsNotEqualToNull() {
        assertNotEquals(validAircraft(), null);
    }

    @Test
    void ensureTwoDifferentAircraftAreNotEqual() {
        assertNotEquals(validAircraft(), validAircraft());
    }

    @Test
    void ensureAircraftSameAsItself() {
        final Aircraft aircraft = validAircraft();
        assertTrue(aircraft.sameAs(aircraft));
    }

    @Test
    void ensureAircraftSameAsNullIsFalse() {
        assertFalse(validAircraft().sameAs(null));
    }

    @Test
    void ensureRegistrationNumberReturnsIdentityString() {
        final Aircraft aircraft = validAircraft();
        assertNotNull(aircraft.registrationNumber());
        assertEquals("CS-TKA", aircraft.registrationNumber());
    }
}
