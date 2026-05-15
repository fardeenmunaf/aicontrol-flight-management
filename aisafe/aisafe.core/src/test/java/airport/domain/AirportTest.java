package airport.domain;

import aircontrolarea.domain.AirControlArea;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirportTest {

    private static final AirControlArea AREA = new AirControlArea("PT-NORTH", 100.0, "North Portugal");
    private static final AirportCoordinates COORDS = new AirportCoordinates(41.24, -8.67);

    private Airport validAirport() {
        return new Airport("Porto Airport", "Porto", "Portugal", "OPO", "LPPR", 69.0, COORDS, AREA);
    }

    // --- Construction ---

    @Test
    void ensureAirportWithValidDataIsCreated() {
        final Airport a = validAirport();
        assertEquals("Porto Airport", a.name());
        assertEquals("Porto", a.town());
        assertEquals("Portugal", a.country());
        assertEquals("OPO", a.iataCode());
        assertEquals("LPPR", a.icaoCode());
        assertEquals(69.0, a.elevation());
        assertNotNull(a.coordinates());
        assertNotNull(a.airControlArea());
        assertNotNull(a.identity());
    }

    @Test
    void ensureIataCodeIsNormalizedToUppercase() {
        final Airport a = new Airport("Test", "City", "Country", "opo", "LPPR", 0.0, COORDS, AREA);
        assertEquals("OPO", a.iataCode());
    }

    @Test
    void ensureIcaoCodeIsNormalizedToUppercase() {
        final Airport a = new Airport("Test", "City", "Country", "OPO", "lppr", 0.0, COORDS, AREA);
        assertEquals("LPPR", a.icaoCode());
    }

    @Test
    void ensureZeroElevationIsAccepted() {
        assertDoesNotThrow(() ->
                new Airport("Test", "City", "Country", "AAA", "AAAA", 0.0, COORDS, AREA));
    }

    // --- IATA validation ---

    @Test
    void ensureIataWithFewerThan3LettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "AB", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void ensureIataWithMoreThan3LettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "ABCD", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void ensureIataWithDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "A1B", "LPPR", 0.0, COORDS, AREA));
    }

    // --- ICAO validation ---

    @Test
    void ensureIcaoWithFewerThan4LettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPP", 0.0, COORDS, AREA));
    }

    @Test
    void ensureIcaoWithMoreThan4LettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPRT", 0.0, COORDS, AREA));
    }

    @Test
    void ensureIcaoWithDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "L1PR", 0.0, COORDS, AREA));
    }

    // --- Elevation ---

    @Test
    void ensureNegativeElevationIsAllowed() {
        // Airports below sea level exist (e.g. Schiphol, -4 m)
        final Airport airport = new Airport("Test", "City", "Country", "OPO", "LPPR", -4.0, COORDS, AREA);
        assertEquals(-4.0, airport.elevation());
    }

    // --- Null fields ---

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(Exception.class, () ->
                new Airport(null, "City", "Country", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void ensureNullAreaIsRejected() {
        assertThrows(Exception.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPR", 0.0, COORDS, null));
    }

    @Test
    void ensureNullCoordinatesIsRejected() {
        assertThrows(Exception.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPR", 0.0, null, AREA));
    }

    // --- Empty string fields ---

    @Test
    void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("", "City", "Country", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void ensureEmptyTownIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "", "Country", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void ensureEmptyCountryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    // --- All getters ---

    @Test
    void ensureAllGettersReturnExpectedValues() {
        final Airport a = validAirport();
        assertNotNull(a.airportId());
        assertEquals(a.identity(), a.airportId());
        assertEquals("Porto", a.town());
        assertEquals("Portugal", a.country());
        assertNotNull(a.coordinates().latitude());
        assertNotNull(a.coordinates().longitude());
    }

    // --- sameAs ---

    @Test
    void ensureAirportIsSameAsItself() {
        final Airport a = validAirport();
        assertTrue(a.sameAs(a));
    }

    @Test
    void ensureDifferentAirportsAreNotSameAs() {
        final Airport a1 = new Airport("Porto", "Porto", "PT", "OPO", "LPPR", 69.0, COORDS, AREA);
        final Airport a2 = new Airport("Lisbon", "Lisbon", "PT", "LIS", "LPPT", 114.0, COORDS, AREA);
        assertFalse(a1.sameAs(a2));
    }

    // --- Equality ---

    @Test
    void ensureAirportIsEqualToItself() {
        final Airport a = validAirport();
        assertEquals(a, a);
        assertEquals(a.hashCode(), a.hashCode());
    }

    @Test
    void ensureAirportIsNotEqualToNull() {
        assertNotEquals(validAirport(), null);
    }

    @Test
    void ensureTwoDifferentAirportsAreNotEqual() {
        final Airport a1 = new Airport("Porto", "Porto", "PT", "OPO", "LPPR", 69.0, COORDS, AREA);
        final Airport a2 = new Airport("Lisbon", "Lisbon", "PT", "LIS", "LPPT", 114.0, COORDS, AREA);
        assertNotEquals(a1, a2);
    }
}
