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

    // TC05 – AC08: identificador de voo duplicado
    @Test
    void shouldRejectDuplicateFlightIdentifier() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_duplicate_id.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.type() == SemanticErrorType.SEMANTIC && e.message().contains("TP1234")));
    }

    // TC08 – AC10: origem/destino da rota não corresponde ao primeiro/último leg
    @Test
    void shouldRejectRouteMismatch() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_route_mismatch.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream().anyMatch(e -> e.message().contains("Route origin")));
        assertTrue(result.semanticErrors().stream().anyMatch(e -> e.message().contains("Route destination")));
    }

    // TC09 – visitor e listener ambos invocados
    @Test
    void shouldInvokeBothVisitorAndListenerForValidFlight() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("valid_flight.dsl"));

        // listener (FlightStructureListener)
        assertFalse(result.summary().flightDesignators().isEmpty());
        assertEquals("TP1234", result.summary().flightDesignators().get(0));

        // visitor (FlightAstBuilderVisitor)
        assertNotNull(result.document());
        assertFalse(result.document().flightPlans().isEmpty());
        assertEquals("TP1234", result.document().flightPlans().get(0).flightDesignator());
        assertEquals("atcc1@tap.pt", result.document().flightPlans().get(0).pilotRef());
        assertEquals("OPO", result.document().flightPlans().get(0).routeOrigin());
        assertEquals("MAD", result.document().flightPlans().get(0).routeDestination());
    }

    // TC10 – AC12: combustível zero rejeitado semanticamente
    @Test
    void shouldRejectZeroFuelQuantity() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_zero_fuel.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.type() == SemanticErrorType.SEMANTIC && e.message().contains("strictly positive")));
    }

    // hora de chegada antes da hora de partida
    @Test
    void shouldRejectArrivalBeforeDeparture() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_arrival_before_departure.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Arrival date/time must be after departure")));
    }

    // leg 2 parte antes de leg 1 ter chegado
    @Test
    void shouldRejectLegTimeOverlap() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_leg_time_overlap.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("must be after arrival of leg")));
    }

    // velocidade 0 no climb rejeitada
    @Test
    void shouldRejectInvalidClimbProfileSpeed() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_profile_climb_speed.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Climb profile")));
    }

    // rate >= 0 no descend rejeitada
    @Test
    void shouldRejectInvalidDescendProfileRate() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_profile_descend_rate.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Descend profile")));
    }

    // coordenadas fora dos limites rejeitadas
    @Test
    void shouldRejectOutOfBoundsCoordinates() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_coords_out_of_bounds.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream().anyMatch(e -> e.message().contains("Latitude")));
        assertTrue(result.semanticErrors().stream().anyMatch(e -> e.message().contains("Longitude")));
    }

    // processor lança ParseCancellationException para erros sintáticos
    @Test
    void shouldThrowParseExceptionForSyntaxError() throws Exception {
        assertThrows(ParseCancellationException.class,
                () -> processor.process(resourcePath("invalid_missing_fuel.dsl")));
    }

    // overload process(String) funciona corretamente
    @Test
    void shouldProcessValidDslString() {
        final String seg1 =
                "segment { from: (41.262891, -8.68522, altitude: 69 m); to: (42.0, -8.01, altitude: 9249 m); " +
                "altslot [ min: 0 m, max: 9249 m, width: 50 m ]; wind { direction: 270; speed: 15 m/s; } mode: climb; } ";
        final String seg2 =
                "segment { from: (42.0, -8.01, altitude: 9249 m); to: (41.0, -4.3, altitude: 9249 m); " +
                "altslot [ min: 9000 m, max: 9500 m, width: 50 m ]; wind { direction: 270; speed: 20 m/s; } mode: cruise; } ";
        final String seg3 =
                "segment { from: (41.0, -4.3, altitude: 9249 m); to: (40.4895, -3.5643, altitude: 610 m); " +
                "altslot [ min: 0 m, max: 9249 m, width: 50 m ]; wind { direction: 270; speed: 10 m/s; } mode: descend; } ";
        final String dsl =
                "flight TP0001 { type: regular; aircraft: A320; pilot: atcc1@tap.pt; route: ZZ001 from OPO to MAD; " +
                "leg { departure { airport: OPO; date: 2027-01-01; time: 06:00; } " +
                "arrival { airport: MAD; date: 2027-01-01; time: 08:00; } " +
                "fuel { quantity: 1 kg; } " + seg1 + seg2 + seg3 + "} }";

        final FlightDslProcessingResult result = processor.process(dsl);

        assertTrue(result.isValid());
        assertEquals("TP0001", result.document().flightPlans().get(0).flightDesignator());
    }

    // segment missing wind block is a syntactic error
    @Test
    void shouldThrowParseExceptionForSegmentMissingWind() throws Exception {
        assertThrows(ParseCancellationException.class,
                () -> processor.process(resourcePath("invalid_missing_cruise_segment.dsl")));
    }

    // segment missing altslot is a syntactic error
    @Test
    void shouldThrowParseExceptionForSegmentMissingAltslot() throws Exception {
        assertThrows(ParseCancellationException.class,
                () -> processor.process(resourcePath("invalid_wrong_segment_order.dsl")));
    }

    @Test
    void shouldRejectDepartureInThePast() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_past_departure_date.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("must not be in the past")));
    }

    @Test
    void shouldRejectClimbSegmentWithDecreasingAltitude() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_climb_altitude_not_increasing.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Climb segment") && e.message().contains("increasing altitude")));
    }

    @Test
    void shouldRejectDescendSegmentWithIncreasingAltitude() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_descend_altitude_not_decreasing.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("Descend segment") && e.message().contains("decreasing altitude")));
    }

    @Test
    void shouldRejectExcessiveAltitude() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_altitude_too_high.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.message().contains("exceeds maximum allowed")));
    }

    private Path resourcePath(final String resourceName) throws Exception {
        return Path.of(getClass().getResource("/" + resourceName).toURI());
    }
}
