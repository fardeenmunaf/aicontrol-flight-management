package report.monthly;

import java.util.Map;

public record MonthlyStatistics(
        int year, int month,
        int simulationCount, int flightCount,
        int violationCount, double violationRate,
        int passedCount, int failedCount,
        Map<String, AreaStats> perArea) {

    public record AreaStats(int flights, int violations) {}

    public boolean isEmpty() { return simulationCount == 0 && flightCount == 0; }
}
