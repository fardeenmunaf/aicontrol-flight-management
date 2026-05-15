package flight.domain;

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

import static org.junit.jupiter.api.Assertions.*;

class FlightTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T12:00:00Z");

    private static final AircraftModel MODEL = new AircraftModel(
            AircraftType.PASSENGER,
            40000.0, 75000.0, 65000.0, 20000.0,
            12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
            "B737-Flight", 2,
            new AircraftModelMaker("Boeing", "USA"));

    private static final EngineModel ENGINE = new EngineModel(
            "CFM56-FL", EngineType.TURBOFAN, 121.0, 0.38, 121000.0, 98000.0, 0.62,
            new EngineModelMaker("CFM", "France"),
            new FuelProperties(FuelType.JET_A, 0.80, 43.0));

    static { MODEL.addSupportedEngineModel(ENGINE); }

    private static final AirTransportCompany COMPANY =
            new AirTransportCompany("Flight Airlines", "FL", "FLA");

    private static final CabinConfiguration CABIN = new CabinConfiguration(150, 10, 20);

    private Flight validFlight() {
        return new Flight("TP123", FlightType.REGULAR, NOW, LATER);
    }

    // --- Construction: happy path ---

    @Test
    void ensureFlightIsCreatedWithExpectedState() {
        final Flight flight = validFlight();
        assertAll(
                () -> assertEquals("TP123", flight.flightDesignator()),
                () -> assertEquals(FlightType.REGULAR, flight.type()),
                () -> assertEquals(NOW, flight.scheduledDepartureTime()),
                () -> assertEquals(LATER, flight.scheduledArrivalTime()),
                () -> assertNull(flight.aircraft()),
                () -> assertNotNull(flight.identity())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(null, FlightType.REGULAR, NOW, LATER));
    }

    @Test
    void ensureEmptyDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Flight("", FlightType.REGULAR, NOW, LATER));
    }

    @Test
    void ensureNullTypeIsRejected() {
        assertThrows(Exception.class,
                () -> new Flight("TP123", null, NOW, LATER));
    }

    @Test
    void ensureNullDepartureTimeIsRejected() {
        assertThrows(Exception.class,
                () -> new Flight("TP123", FlightType.REGULAR, null, LATER));
    }

    @Test
    void ensureNullArrivalTimeIsRejected() {
        assertThrows(Exception.class,
                () -> new Flight("TP123", FlightType.REGULAR, NOW, null));
    }

    @Test
    void ensureArrivalBeforeDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Flight("TP123", FlightType.REGULAR, LATER, NOW));
    }

    // --- assignAircraft ---

    @Test
    void ensureActiveAircraftCanBeAssigned() {
        final Flight flight = validFlight();
        final Aircraft aircraft = new Aircraft("CS-TKA", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY);
        flight.assignAircraft(aircraft);
        assertEquals(aircraft, flight.aircraft());
    }

    @Test
    void ensureNullAircraftIsRejected() {
        final Flight flight = validFlight();
        assertThrows(IllegalArgumentException.class, () -> flight.assignAircraft(null));
    }

    @Test
    void ensureDecommissionedAircraftCannotBeAssigned() {
        final Flight flight = validFlight();
        final Aircraft aircraft = new Aircraft("CS-DEC", "Portugal",
                AircraftOperationStatus.IN_SERVICE, 2, MODEL, ENGINE, CABIN, COMPANY);
        aircraft.decommission();
        assertThrows(IllegalStateException.class, () -> flight.assignAircraft(aircraft));
    }

    // --- equals / hashCode ---

    @Test
    void ensureFlightIsEqualToItself() {
        final Flight flight = validFlight();
        assertEquals(flight, flight);
        assertEquals(flight.hashCode(), flight.hashCode());
    }

    @Test
    void ensureFlightIsNotEqualToNull() {
        assertNotEquals(validFlight(), null);
    }

    @Test
    void ensureTwoDifferentFlightsAreNotEqual() {
        final Flight f1 = validFlight();
        final Flight f2 = validFlight();
        assertNotEquals(f1, f2);
    }

    @Test
    void ensureFlightSameAsItself() {
        final Flight flight = validFlight();
        assertTrue(flight.sameAs(flight));
    }

    @Test
    void ensureFlightSameAsNullIsFalse() {
        final Flight flight = validFlight();
        assertFalse(flight.sameAs(null));
    }
}
