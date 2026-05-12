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
    void shouldCreateAirportWithValidData() {
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
    void shouldNormalizeIataToUppercase() {
        final Airport a = new Airport("Test", "City", "Country", "opo", "LPPR", 0.0, COORDS, AREA);
        assertEquals("OPO", a.iataCode());
    }

    @Test
    void shouldNormalizeIcaoToUppercase() {
        final Airport a = new Airport("Test", "City", "Country", "OPO", "lppr", 0.0, COORDS, AREA);
        assertEquals("LPPR", a.icaoCode());
    }

    @Test
    void shouldAcceptZeroElevation() {
        assertDoesNotThrow(() ->
                new Airport("Test", "City", "Country", "AAA", "AAAA", 0.0, COORDS, AREA));
    }

    // --- IATA validation ---

    @Test
    void shouldThrowWhenIataHasFewerThan3Letters() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "AB", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenIataHasMoreThan3Letters() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "ABCD", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenIataContainsDigits() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "A1B", "LPPR", 0.0, COORDS, AREA));
    }

    // --- ICAO validation ---

    @Test
    void shouldThrowWhenIcaoHasFewerThan4Letters() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPP", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenIcaoHasMoreThan4Letters() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPRT", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenIcaoContainsDigits() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "L1PR", 0.0, COORDS, AREA));
    }

    // --- Elevation ---

    @Test
    void shouldThrowWhenElevationIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPR", -1.0, COORDS, AREA));
    }

    // --- Null fields ---

    @Test
    void shouldThrowWhenNameIsNull() {
        assertThrows(Exception.class, () ->
                new Airport(null, "City", "Country", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenAreaIsNull() {
        assertThrows(Exception.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPR", 0.0, COORDS, null));
    }

    @Test
    void shouldThrowWhenCoordinatesIsNull() {
        assertThrows(Exception.class, () ->
                new Airport("Test", "City", "Country", "OPO", "LPPR", 0.0, null, AREA));
    }

    // --- Empty string fields (distinct from null — triggers StringPredicates branch) ---

    @Test
    void shouldThrowWhenNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("", "City", "Country", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenTownIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "", "Country", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    @Test
    void shouldThrowWhenCountryIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport("Test", "City", "", "OPO", "LPPR", 0.0, COORDS, AREA));
    }

    // --- All getters ---

    @Test
    void shouldExposeAllGetters() {
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
    void shouldBeSameAsItself() {
        final Airport a = validAirport();
        assertTrue(a.sameAs(a));
    }

    @Test
    void shouldNotBeSameAsDifferentAirport() {
        final Airport a1 = new Airport("Porto", "Porto", "PT", "OPO", "LPPR", 69.0, COORDS, AREA);
        final Airport a2 = new Airport("Lisbon", "Lisbon", "PT", "LIS", "LPPT", 114.0, COORDS, AREA);
        assertFalse(a1.sameAs(a2));
    }

    // --- Equality ---

    @Test
    void shouldBeEqualToItself() {
        final Airport a = validAirport();
        assertEquals(a, a);
        assertEquals(a.hashCode(), a.hashCode());
    }

    @Test
    void shouldNotBeEqualToNull() {
        assertNotEquals(validAirport(), null);
    }

    @Test
    void twoDifferentAirportsShouldNotBeEqual() {
        final Airport a1 = new Airport("Porto", "Porto", "PT", "OPO", "LPPR", 69.0, COORDS, AREA);
        final Airport a2 = new Airport("Lisbon", "Lisbon", "PT", "LIS", "LPPT", 114.0, COORDS, AREA);
        assertNotEquals(a1, a2);
    }
}
