package report.monthly;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MonthlyStatistics(
        int year, int month,
        int simulationCount, int flightCount,
        int violationCount, double violationRate,
        int passedCount, int failedCount,
        Map<String, AreaStats> perArea,
        List<ViolationDetail> violationDetails) {

    public record AreaStats(int flights, int violations) {}

    /**
     * A single safety-violation entry for the detail section of the report.
     *
     * @param timestamp         UTC instant when the violation occurred
     * @param flightCallsign    callsign of the SimulationFlight that triggered the violation
     * @param violationType     e.g. COLLISION or FUEL_EXHAUSTION
     * @param involvedCallsigns all aircraft callsigns involved (may be empty)
     * @param area              air-control-area code, or "Unknown"
     */
    public record ViolationDetail(
            Instant timestamp,
            String flightCallsign,
            String violationType,
            String involvedCallsigns,
            String area) {}

    public boolean isEmpty() { return simulationCount == 0 && flightCount == 0; }
}
