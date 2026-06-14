package simulation.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import simulation.domain.ViolationType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Parses the C engine's simulation_report_*.json into a SimulationResultDTO. */
public class SimulationResultParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public SimulationResultDTO parse(final String json) {
        try {
            final JsonNode root = mapper.readTree(json);
            final Instant start = parseInstant(root.path("simulation").path("wallClockStart").asText(null));

            final List<SimulationResultDTO.FlightOutcome> flights = new ArrayList<>();
            for (final JsonNode f : root.path("flightExecutionStatuses")) {
                flights.add(new SimulationResultDTO.FlightOutcome(
                        f.path("callsign").asText(),
                        f.path("completed").asBoolean(),
                        f.path("hadViolation").asBoolean()));
            }

            final List<SimulationResultDTO.ViolationOutcome> violations = new ArrayList<>();
            for (final JsonNode v : root.path("violationEvents")) {
                final List<String> callsigns = new ArrayList<>();
                final String a = v.path("aircraftA").path("callsign").asText(null);
                final String b = v.path("aircraftB").path("callsign").asText(null);
                if (a != null) callsigns.add(a);
                if (b != null && !b.equals(a)) callsigns.add(b);
                // Same callsign on both sides = the aircraft ran out of fuel.
                final ViolationType type = (b != null && b.equals(a))
                        ? ViolationType.FUEL_EXHAUSTION
                        : ViolationType.COLLISION;
                violations.add(new SimulationResultDTO.ViolationOutcome(
                        parseInstant(v.path("wallClockTime").asText(null)), type, callsigns));
            }
            return new SimulationResultDTO(start, start, flights, violations);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid simulation report JSON: " + e.getMessage(), e);
        }
    }

    private Instant parseInstant(final String s) {
        if (s == null || s.isBlank()) return Instant.EPOCH;
        return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC);
    }
}
