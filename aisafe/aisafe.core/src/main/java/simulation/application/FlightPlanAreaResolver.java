package simulation.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import aircontrolarea.domain.AirControlArea;
import airport.repositories.AirportRepository;

import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves a flight's air control area and departure date from the
 * flightplansSim/*.json files.
 *
 * Area resolution: reads {@code route.origin} (IATA code) from each plan and
 * looks up
 * the matching {@link airport.domain.Airport} via
 * {@link AirportRepository#findByIataCode}.
 * This is robust and independent of FlightRoute designation naming conventions.
 *
 * Departure-date resolution: reads {@code legs[0].departure.date} (ISO-8601
 * local date)
 * and converts it to an {@link Instant} at midnight UTC, so each flight has its
 * real
 * scheduled departure rather than the simulation wall-clock start.
 */
public class FlightPlanAreaResolver
        implements RecordSimulationResultService.AreaResolver,
        RecordSimulationResultService.DepartureDateResolver {

    private final Map<String, String> callsignToOriginIata = new HashMap<>();
    private final Map<String, Instant> callsignToDepartureDate = new HashMap<>();
    private final AirportRepository airports;

    public FlightPlanAreaResolver(final Path flightPlanDir, final AirportRepository airports) {
        this.airports = airports;
        final ObjectMapper mapper = new ObjectMapper();
        try (var stream = Files.newDirectoryStream(flightPlanDir, "*.json")) {
            for (final Path p : stream) {
                final JsonNode arr = mapper.readTree(Files.readString(p));
                final JsonNode fp = arr.isArray() ? arr.get(0) : arr;

                final String callsign = fp.path("flightDesignator").asText(null);
                final String originIata = fp.path("route").path("origin").asText(null);
                if (callsign == null)
                    continue;

                if (originIata != null) {
                    callsignToOriginIata.put(callsign, originIata);
                }

                // Extract the departure date from the first leg (e.g. "2026-06-23").
                final JsonNode firstLeg = fp.path("legs").path(0);
                final String dateStr = firstLeg.path("departure").path("date").asText(null);
                if (dateStr != null) {
                    try {
                        final Instant dep = LocalDate.parse(dateStr)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant();
                        callsignToDepartureDate.put(callsign, dep);
                    } catch (final Exception ignored) {
                        /* malformed date — fall back to sim start */ }
                }
            }
        } catch (final Exception ignored) {
            /* directory absent; both maps stay empty */ }
    }

    @Override
    public AirControlArea areaFor(final String callsign) {
        final String iata = callsignToOriginIata.get(callsign);
        if (iata == null)
            return null;
        try {
            return airports.findByIataCode(iata)
                    .map(airport.domain.Airport::airControlArea)
                    .orElse(null);
        } catch (final RuntimeException e) {
            return null;
        }
    }

    @Override
    public Instant departureDateFor(final String callsign, final Instant fallback) {
        return callsignToDepartureDate.getOrDefault(callsign, fallback);
    }
}
