package simulation.application;

import aircontrolarea.domain.AirControlArea;
import airport.domain.Airport;
import airport.repositories.AirportRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.repository.FlightPlanRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves a flight's air control area and scheduled departure date from the
 * {@link FlightPlan} records stored in the database (seeded from the simulation
 * flight plans). Replaces the file-based resolver — no flightplansSim/*.json
 * reading and no working-directory dependency.
 *
 * <ul>
 *   <li>Area: {@code FlightPlan.routeOrigin} (IATA) -&gt; {@link Airport} -&gt; {@link AirControlArea}.</li>
 *   <li>Departure date: {@code FlightPlan.departureTime}.</li>
 * </ul>
 */
public class DbFlightPlanResolver
        implements RecordSimulationResultService.AreaResolver,
                   RecordSimulationResultService.DepartureDateResolver {

    private final FlightPlanRepository flightPlans;
    private final AirportRepository airports;
    private final Map<String, Optional<FlightPlan>> cache = new HashMap<>();

    public DbFlightPlanResolver(final FlightPlanRepository flightPlans, final AirportRepository airports) {
        this.flightPlans = flightPlans;
        this.airports = airports;
    }

    private Optional<FlightPlan> planFor(final String callsign) {
        return cache.computeIfAbsent(callsign, cs -> {
            try {
                for (final FlightPlan fp : flightPlans.findByFlightDesignator(cs)) {
                    return Optional.of(fp); // first match wins
                }
            } catch (final RuntimeException ignored) { /* fall through to empty */ }
            return Optional.empty();
        });
    }

    @Override
    public AirControlArea areaFor(final String callsign) {
        return planFor(callsign)
                .map(FlightPlan::routeOrigin)
                .flatMap(iata -> {
                    try {
                        return airports.findByIataCode(iata);
                    } catch (final RuntimeException e) {
                        return Optional.<Airport>empty();
                    }
                })
                .map(Airport::airControlArea)
                .orElse(null);
    }

    @Override
    public Instant departureDateFor(final String callsign, final Instant fallback) {
        return planFor(callsign)
                .map(FlightPlan::departureTime)
                .orElse(fallback);
    }
}
