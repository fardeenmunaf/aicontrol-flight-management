package flightPlan.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegAndSegmentImportRequestTest {

    @Test
    void ensureSegmentImportRequestStoresCoordinates() {
        final SegmentImportRequest seg = new SegmentImportRequest(41.26, -8.68, 40.49, -3.56);
        assertEquals(41.26, seg.fromLatitude);
        assertEquals(-8.68, seg.fromLongitude);
        assertEquals(40.49, seg.toLatitude);
        assertEquals(-3.56, seg.toLongitude);
    }

    @Test
    void ensureLegImportRequestStoresAllFields() {
        final SegmentImportRequest seg = new SegmentImportRequest(41.26, -8.68, 40.49, -3.56);
        final LegImportRequest leg = new LegImportRequest(
                1, "2026-05-05", "08:00", "2026-05-05", "09:30",
                40200.0, "OPO", "MAD", List.of(seg));

        assertEquals(1, leg.sequenceNumber);
        assertEquals("2026-05-05", leg.departureDate);
        assertEquals("08:00", leg.departureTime);
        assertEquals("2026-05-05", leg.arrivalDate);
        assertEquals("09:30", leg.arrivalTime);
        assertEquals(40200.0, leg.fuelQuantity);
        assertEquals("OPO", leg.departureAirportCode);
        assertEquals("MAD", leg.arrivalAirportCode);
        assertEquals(1, leg.segments.size());
    }

    @Test
    void ensureLegImportRequestRejectsNullDepartureDate() {
        assertThrows(NullPointerException.class, () ->
                new LegImportRequest(1, null, "08:00", "2026-05-05", "09:30",
                        40200.0, "OPO", "MAD", List.of()));
    }

    @Test
    void ensureLegImportRequestRejectsNullSegments() {
        assertThrows(NullPointerException.class, () ->
                new LegImportRequest(1, "2026-05-05", "08:00", "2026-05-05", "09:30",
                        40200.0, "OPO", "MAD", null));
    }

    @Test
    void ensureLegImportRequestSegmentsListIsUnmodifiable() {
        final LegImportRequest leg = new LegImportRequest(
                1, "2026-05-05", "08:00", "2026-05-05", "09:30",
                40200.0, "OPO", "MAD",
                List.of(new SegmentImportRequest(1, 2, 3, 4)));
        assertThrows(UnsupportedOperationException.class, () ->
                leg.segments.add(new SegmentImportRequest(5, 6, 7, 8)));
    }
}
