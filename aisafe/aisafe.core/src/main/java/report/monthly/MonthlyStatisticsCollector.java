package report.monthly;

import aircontrolarea.domain.AirControlArea;
import simulation.domain.SafetyViolation;
import simulation.domain.Simulation;
import simulation.domain.SimulationFlight;
import simulation.repository.SimulationRepository;

import java.time.*;
import java.util.*;

public class MonthlyStatisticsCollector {

    private final SimulationRepository simulations;

    public MonthlyStatisticsCollector(final SimulationRepository simulations) {
        this.simulations = simulations;
    }

    public MonthlyStatistics collect(final int year, final int month) {
        final Instant from = YearMonth.of(year, month).atDay(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant to = YearMonth.of(year, month).plusMonths(1).atDay(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        final List<Simulation> sims = simulations.findByStartDateTimeBetween(from, to);

        int flights = 0, passed = 0, failed = 0, violations = 0;
        final Map<String, int[]> perArea = new TreeMap<>(); // area -> [flights, violations]

        for (final Simulation s : sims) {
            for (final SimulationFlight f : s.simulationFlights()) {
                flights++;
                if (f.passedValidation()) passed++; else failed++;
                final String area = areaName(f.airControlArea());
                perArea.computeIfAbsent(area, k -> new int[2])[0]++;
            }
            for (final SafetyViolation v : s.safetyViolations()) {
                violations++;
                final String area = areaName(v.airControlArea());
                perArea.computeIfAbsent(area, k -> new int[2])[1]++;
            }
        }

        final double rate = flights == 0 ? 0.0 : (double) violations / flights;
        final Map<String, MonthlyStatistics.AreaStats> areaStats = new TreeMap<>();
        perArea.forEach((k, v) -> areaStats.put(k, new MonthlyStatistics.AreaStats(v[0], v[1])));

        return new MonthlyStatistics(year, month, sims.size(), flights, violations, rate,
                passed, failed, areaStats);
    }

    private String areaName(final AirControlArea area) {
        return area == null ? "Unknown" : area.areaCode();
    }
}
