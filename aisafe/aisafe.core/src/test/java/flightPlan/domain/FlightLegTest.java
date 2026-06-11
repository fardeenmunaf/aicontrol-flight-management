package flightPlan.domain;

import org.junit.jupiter.api.Test;
import shared.domain.AirportId;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FlightLegTest {

    private static final Instant DEP = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant ARR = Instant.parse("2026-01-01T10:00:00Z");
    private static final AirportId LISBON = AirportId.valueOf("LIS");
    private static final AirportId PORTO = AirportId.valueOf("OPO");

    private FlightLeg validLeg() {
        return new FlightLeg(1, DEP, ARR, 5000.0, LISBON, PORTO);
    }

    // --- Construction: happy path ---

    @Test
    void ensureFlightLegIsCreatedWithExpectedState() {
        final FlightLeg leg = validLeg();
        assertAll(
                () -> assertEquals(1, leg.sequenceNumber()),
                () -> assertEquals(DEP, leg.departureTime()),
                () -> assertEquals(ARR, leg.arrivalTime()),
                () -> assertEquals(5000.0, leg.fuelQuantity()),
                () -> assertEquals(LISBON, leg.departureAirportId()),
                () -> assertEquals(PORTO, leg.arrivalAirportId()),
                () -> assertTrue(leg.segments().isEmpty())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureZeroSequenceNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(0, DEP, ARR, 5000.0, LISBON, PORTO));
    }

    @Test
    void ensureNegativeSequenceNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(-1, DEP, ARR, 5000.0, LISBON, PORTO));
    }

    @Test
    void ensureNullDepartureTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(1, null, ARR, 5000.0, LISBON, PORTO));
    }

    @Test
    void ensureNullArrivalTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(1, DEP, null, 5000.0, LISBON, PORTO));
    }

    @Test
    void ensureArrivalBeforeDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg(1, ARR, DEP, 5000.0, LISBON, PORTO));
    }

    @Test
    void ensureArrivalEqualToDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg(1, DEP, DEP, 5000.0, LISBON, PORTO));
    }

    @Test
    void ensureZeroFuelQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(1, DEP, ARR, 0.0, LISBON, PORTO));
    }

    @Test
    void ensureNegativeFuelQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(1, DEP, ARR, -1.0, LISBON, PORTO));
    }

    @Test
    void ensureNullDepartureAirportIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(1, DEP, ARR, 5000.0, null, PORTO));
    }

    @Test
    void ensureNullArrivalAirportIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightLeg(1, DEP, ARR, 5000.0, LISBON, null));
    }

    // --- addSegment ---

    @Test
    void ensureSegmentCanBeAdded() {
        final FlightLeg leg = validLeg();
        final shared.domain.NodeId n1 = shared.domain.NodeId.newId();
        final shared.domain.NodeId n2 = shared.domain.NodeId.newId();
        final Segment seg = new Segment(n1, n2, 90.0, 5.0);
        leg.addSegment(seg);
        assertEquals(1, leg.segments().size());
    }

    // --- equals / hashCode ---

    @Test
    void ensureLegIsEqualToItself() {
        final FlightLeg leg = validLeg();
        assertEquals(leg, leg);
    }

    @Test
    void ensureLegIsNotEqualToNull() {
        assertNotEquals(validLeg(), null);
    }

    @Test
    void ensureLegHashCodeIsConsistent() {
        final FlightLeg leg = validLeg();
        assertEquals(leg.hashCode(), leg.hashCode());
    }

    @Test
    void ensureLegIdentityIsNullBeforePersistence() {
        final FlightLeg leg = validLeg();
        assertNull(leg.identity());
    }

    @Test
    void ensureLegSameAsItself() {
        final FlightLeg leg = validLeg();
        assertTrue(leg.sameAs(leg));
    }

    @Test
    void ensureLegSameAsNullIsFalse() {
        final FlightLeg leg = validLeg();
        assertFalse(leg.sameAs(null));
    }

    @Test
    void ensureNullSegmentIsRejectedFromAddSegment() {
        final FlightLeg leg = validLeg();
        assertThrows(IllegalArgumentException.class, () -> leg.addSegment(null));
    }

    @Test
    void ensureProfileEntriesCanBeAdded() {
        final FlightLeg leg = validLeg();
        leg.addProfileEntry(ProfileEntry.climb(1000.0, "m", 210.0, "kts"));
        leg.addProfileEntry(ProfileEntry.cruise(460.0, "kts"));
        assertEquals(2, leg.profileEntries().size());
    }

    @Test
    void ensureNullProfileEntryIsRejected() {
        final FlightLeg leg = validLeg();
        assertThrows(IllegalArgumentException.class, () -> leg.addProfileEntry(null));
    }
}
