package flight.dsl;

import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flight.dsl.validation.SemanticErrorType;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlightDslProcessorTest {

    private final FlightDslProcessor processor = new FlightDslProcessor();

    // TC01
    @Test
    void shouldBuildAstAndValidateValidFlight() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("valid_flight.dsl"));

        assertTrue(result.isValid());
        assertNotNull(result.document());
        assertEquals(1, result.summary().flightPlanCount());
        assertEquals(1, result.summary().legCount());
        assertEquals(3, result.summary().segmentCount());
    }

    // TC04 + TC07
    @Test
    void shouldCollectSemanticErrorsForDuplicateAirports() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_duplicate_airport.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
    }

    // TC04 + TC06
    @Test
    void shouldCollectSemanticErrorsForLegMismatch() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_leg_mismatch.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
    }

    @Test
    void shouldCollectSemanticErrorsForIdenticalSegmentCoordinates() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_same_segment_coords.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
    }

    // TC05 – AC08: duplicate flight identifier rejected
    @Test
    void shouldRejectDuplicateFlightIdentifier() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_duplicate_id.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.type() == SemanticErrorType.SEMANTIC && e.message().contains("TP1234")));
    }

    // TC08 – AC10: route origin/destination must match first/last leg airports
    @Test
    void shouldRejectRouteMismatch() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_route_mismatch.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Route origin")));
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Route destination")));
    }

    // TC09 – both visitor and listener are exercised on a valid file
    @Test
    void shouldInvokeBothVisitorAndListenerForValidFlight() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("valid_flight.dsl"));

        // listener assertions (FlightStructureListener)
        assertFalse(result.summary().flightDesignators().isEmpty());
        assertEquals("TP1234", result.summary().flightDesignators().get(0));

        // visitor assertions (FlightAstBuilderVisitor)
        assertNotNull(result.document());
        assertFalse(result.document().flightPlans().isEmpty());
        assertEquals("TP1234", result.document().flightPlans().get(0).flightDesignator());
        assertEquals("OPO", result.document().flightPlans().get(0).routeOrigin());
        assertEquals("MAD", result.document().flightPlans().get(0).routeDestination());
    }

    // TC10 – AC12: zero fuel quantity rejected as semantic error
    @Test
    void shouldRejectZeroFuelQuantity() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_zero_fuel.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.type() == SemanticErrorType.SEMANTIC && e.message().contains("strictly positive")));
    }

    // Arrival before departure time rejected
    @Test
    void shouldRejectArrivalBeforeDeparture() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_arrival_before_departure.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Arrival date/time must be after departure")));
    }

    // Next leg departs before previous leg arrives
    @Test
    void shouldRejectLegTimeOverlap() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_leg_time_overlap.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("must be after arrival of leg")));
    }

    // Climb profile with speed = 0 rejected
    @Test
    void shouldRejectInvalidClimbProfileSpeed() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_profile_climb_speed.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Climb profile")));
    }

    // Descend profile with rate = 0 (non-negative) rejected
    @Test
    void shouldRejectInvalidDescendProfileRate() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_profile_descend_rate.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Descend profile")));
    }

    // Out-of-bounds coordinates rejected
    @Test
    void shouldRejectOutOfBoundsCoordinates() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_coords_out_of_bounds.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Latitude")));
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Longitude")));
    }

    // Processor throws ParseCancellationException for syntactically invalid input
    @Test
    void shouldThrowParseExceptionForSyntaxError() throws Exception {
        assertThrows(ParseCancellationException.class,
                () -> processor.process(resourcePath("invalid_missing_fuel.dsl")));
    }

    // process(String) overload works correctly
    @Test
    void shouldProcessValidDslString() {
        final String dsl =
                "flight TP0001 { type: regular; route: ZZ001 from OPO to MAD; " +
                "leg { departure { airport: OPO; date: 2026-01-01; time: 06:00; } " +
                "arrival { airport: MAD; date: 2026-01-01; time: 08:00; } " +
                "fuel { quantity: 1 kg; } " +
                "segment { mode: cruise; from: (41.26, -8.68, altitude: 69 m); to: (40.49, -3.56, altitude: 610 m); } } }";

        final FlightDslProcessingResult result = processor.process(dsl);

        assertTrue(result.isValid());
        assertEquals("TP0001", result.document().flightPlans().get(0).flightDesignator());
    }

    private Path resourcePath(final String resourceName) throws Exception {
        return Path.of(getClass().getResource("/" + resourceName).toURI());
    }
}

