package aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirControlAreaTest {

    // Square polygon centered around (0,0): corners at (1,1),(1,-1),(-1,-1),(-1,1)
    private static final String SQUARE_BOUNDARY = "1.0,1.0;1.0,-1.0;-1.0,-1.0;-1.0,1.0";

    private AirControlArea validArea() {
        return new AirControlArea("PT-NORTH", 500.0, SQUARE_BOUNDARY);
    }

    // --- Construction: happy path ---

    @Test
    void ensureAirControlAreaIsCreatedWithExpectedState() {
        final AirControlArea area = validArea();
        assertAll(
                () -> assertEquals("PT-NORTH", area.areaCode()),
                () -> assertEquals(500.0, area.minimumRequiredArrivalFuel()),
                () -> assertEquals(SQUARE_BOUNDARY, area.geographicBoundaries()),
                () -> assertNotNull(area.identity())
        );
    }

    @Test
    void ensureZeroMinimumFuelIsAccepted() {
        assertDoesNotThrow(() -> new AirControlArea("PT-SOUTH", 0.0, SQUARE_BOUNDARY));
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(null, 100.0, SQUARE_BOUNDARY));
    }

    @Test
    void ensureEmptyAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("", 100.0, SQUARE_BOUNDARY));
    }

    @Test
    void ensureNegativeMinimumFuelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-WEST", -1.0, SQUARE_BOUNDARY));
    }

    @Test
    void ensureNullGeographicBoundariesIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-EAST", 100.0, null));
    }

    @Test
    void ensureEmptyGeographicBoundariesIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-EAST", 100.0, ""));
    }

    // --- containsPoint ---

    @Test
    void ensurePointInsidePolygonIsDetected() {
        final AirControlArea area = validArea();
        assertTrue(area.containsPoint(0.0, 0.0));
    }

    @Test
    void ensurePointOutsidePolygonIsNotDetected() {
        final AirControlArea area = validArea();
        assertFalse(area.containsPoint(5.0, 5.0));
    }

    @Test
    void ensurePointNearEdgeOutsidePolygonIsNotDetected() {
        final AirControlArea area = validArea();
        assertFalse(area.containsPoint(2.0, 0.0));
    }

    @Test
    void ensurePointInsideTriangleIsDetected() {
        // Triangle: (0,0),(0,4),(4,0)
        final AirControlArea area = new AirControlArea("TRI", 0.0, "0.0,0.0;0.0,4.0;4.0,0.0");
        assertTrue(area.containsPoint(1.0, 1.0));
    }

    @Test
    void ensureContainsPointReturnsFalseForMalformedBoundaries() {
        final AirControlArea area = new AirControlArea("BAD", 0.0, "not-valid-boundaries");
        assertFalse(area.containsPoint(0.0, 0.0));
    }

    @Test
    void ensureContainsPointReturnsFalseForTooFewPoints() {
        // Only 2 coordinate pairs - not a polygon
        final AirControlArea area = new AirControlArea("SHORT", 0.0, "1.0,1.0;2.0,2.0");
        assertFalse(area.containsPoint(1.5, 1.5));
    }

    // --- equals / hashCode ---

    @Test
    void ensureAreaIsEqualToItself() {
        final AirControlArea area = validArea();
        assertEquals(area, area);
        assertEquals(area.hashCode(), area.hashCode());
    }

    @Test
    void ensureAreaIsNotEqualToNull() {
        assertNotEquals(validArea(), null);
    }

    @Test
    void ensureTwoDifferentAreasAreNotEqual() {
        final AirControlArea a1 = validArea();
        final AirControlArea a2 = new AirControlArea("PT-SOUTH", 100.0, SQUARE_BOUNDARY);
        assertNotEquals(a1, a2);
    }

    // --- toString ---

    @Test
    void ensureToStringContainsAreaCode() {
        final AirControlArea area = validArea();
        assertTrue(area.toString().contains("PT-NORTH"));
    }

    @Test
    void ensureAreaSameAsItself() {
        final AirControlArea area = validArea();
        assertTrue(area.sameAs(area));
    }

    @Test
    void ensureAreaSameAsNullIsFalse() {
        final AirControlArea area = validArea();
        assertFalse(area.sameAs(null));
    }
}
