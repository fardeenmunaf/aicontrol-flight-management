package flight.dsl;

import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlightDslProcessorTest {

    private final FlightDslProcessor processor = new FlightDslProcessor();

    @Test
    void shouldBuildAstAndValidateValidFlight() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("valid_flight.dsl"));

        assertTrue(result.isValid());
        assertNotNull(result.document());
        assertEquals(1, result.summary().flightPlanCount());
        assertEquals(1, result.summary().legCount());
        assertEquals(3, result.summary().segmentCount());
    }

    @Test
    void shouldCollectSemanticErrorsForDuplicateAirports() throws Exception {
        final FlightDslProcessingResult result = processor.process(resourcePath("invalid_duplicate_airport.dsl"));

        assertFalse(result.semanticErrors().isEmpty());
    }

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

    private Path resourcePath(final String resourceName) throws Exception {
        return Path.of(getClass().getResource("/" + resourceName).toURI());
    }
}

