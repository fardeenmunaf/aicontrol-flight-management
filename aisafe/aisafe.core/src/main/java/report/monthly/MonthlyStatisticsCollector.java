package report.monthly;

import aircontrolarea.domain.AirControlArea;
import simulation.domain.SafetyViolation;
import simulation.domain.Simulation;
import simulation.domain.SimulationFlight;
import simulation.repository.SimulationRepository;
import flightPlan.domain.FlightPlan;
import flightPlan.repository.FlightPlanRepository;
import aircraft.domain.Aircraft;
import aircraft.repositories.AircraftRepository;
import shared.domain.AircraftId;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MonthlyStatisticsCollector {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final SimulationRepository simulations;
    private final FlightPlanRepository flightPlans;
    private final AircraftRepository aircraftRepo;

    public MonthlyStatisticsCollector(final SimulationRepository simulations,
                                      final FlightPlanRepository flightPlans,
                                      final AircraftRepository aircraftRepo) {
        this.simulations = simulations;
        this.flightPlans = flightPlans;
        this.aircraftRepo = aircraftRepo;
    }

    public MonthlyStatistics collect(final int year, final int month) {
        final Instant from = YearMonth.of(year, month).atDay(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant to = YearMonth.of(year, month).plusMonths(1).atDay(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        // Look at ALL simulations and filter each record by its own date: a single
        // multi-month run contributes flights/violations to whichever month they fall in
        // (a run started in June still counts its July flights toward the July report).
        final Iterable<Simulation> sims = simulations.findAll();

        int simulationsCount = 0, flightsCount = 0, passed = 0, failed = 0, violationsCount = 0;
        final Map<String, int[]> perArea = new TreeMap<>(); // area -> [flights, violations]
        final List<MonthlyStatistics.ViolationDetail> details = new ArrayList<>();

        for (final Simulation s : sims) {
            boolean contributed = false; // did this simulation have any activity in the month?
            // Build an index of in-window flights for this simulation (callsign -> flight).
            final Map<String, SimulationFlight> flightByCallsign = new LinkedHashMap<>();
            for (final SimulationFlight f : s.simulationFlights()) {
                // Only count flights whose departure date falls within the requested month.
                if (f.departureDate() == null
                        || f.departureDate().isBefore(from)
                        || !f.departureDate().isBefore(to)) {
                    continue;
                }
                flightsCount++;
                contributed = true;
                if (f.passedValidation()) passed++; else failed++;
                final String area = areaName(f.airControlArea());
                perArea.computeIfAbsent(area, k -> new int[2])[0]++;
                flightByCallsign.put(f.callsign(), f);
            }
            for (final SafetyViolation v : s.safetyViolations()) {
                // Only count violations whose timestamp falls within the requested month.
                if (v.timestamp() == null
                        || v.timestamp().isBefore(from)
                        || !v.timestamp().isBefore(to)) {
                    continue;
                }
                violationsCount++;
                contributed = true;
                final String area = areaName(v.airControlArea());
                perArea.computeIfAbsent(area, k -> new int[2])[1]++;

                // Resolve which flight(s) are involved via the violation's involvedCallsigns.
                // If the violation lists specific callsigns that match known flights, use them;
                // otherwise fall back to listing all in-window flights for the simulation.
                final Set<String> involved = v.involvedCallsigns();
                String flightCallsign;
                if (!involved.isEmpty()) {
                    flightCallsign = involved.stream().sorted().collect(Collectors.joining(", "));
                } else if (!flightByCallsign.isEmpty()) {
                    flightCallsign = flightByCallsign.keySet().stream()
                            .sorted().collect(Collectors.joining(", "));
                } else {
                    flightCallsign = "—";
                }

                final String involvedAircraft;
                if (involved.isEmpty()) {
                    involvedAircraft = "—";
                } else {
                    involvedAircraft = involved.stream().sorted()
                            .map(this::aircraftLabel)
                            .collect(Collectors.joining(" | "));
                }

                details.add(new MonthlyStatistics.ViolationDetail(
                        v.timestamp(),
                        flightCallsign,
                        v.violationType() != null ? v.violationType().name() : "—",
                        involvedAircraft,
                        area));
            }
            if (contributed) simulationsCount++;
        }

        // Sort details chronologically.
        details.sort(Comparator.comparing(MonthlyStatistics.ViolationDetail::timestamp));

        final double rate = flightsCount == 0 ? 0.0 : (double) violationsCount / flightsCount;
        final Map<String, MonthlyStatistics.AreaStats> areaStats = new TreeMap<>();
        perArea.forEach((k, v) -> areaStats.put(k, new MonthlyStatistics.AreaStats(v[0], v[1])));

        return new MonthlyStatistics(year, month, simulationsCount, flightsCount, violationsCount, rate,
                passed, failed, areaStats, details);
    }

    private String areaName(final AirControlArea area) {
        return area == null ? "Unknown" : area.areaCode();
    }

    /**
     * Resolves a callsign to a rich aircraft label via FlightPlan -> aircraftId -> Aircraft:
     * "CALLSIGN (aircraftId, modelId, Maker ModelName)". Falls back to the bare callsign
     * when no flight plan / aircraft / model is found.
     */
    private String aircraftLabel(final String callsign) {
        for (final FlightPlan fp : flightPlans.findByFlightDesignator(callsign)) {
            final String acId = fp.aircraftId();
            if (acId == null) {
                break;
            }
            final Aircraft a = aircraftRepo.ofIdentity(AircraftId.valueOf(acId)).orElse(null);
            if (a != null && a.aircraftModel() != null && a.aircraftModel().maker() != null) {
                return callsign + " (" + a.identity() + ", " + a.aircraftModel().identity() + ", "
                        + a.aircraftModel().maker().name() + " " + a.aircraftModel().modelName() + ")";
            }
            break; // first plan only
        }
        return callsign;
    }
}
