package report.monthly;

import report.framework.Report;
import report.framework.ReportSection;
import report.framework.render.*;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

public class MonthlyStatisticsReport extends Report {

    private final MonthlyStatistics stats;

    public MonthlyStatisticsReport(final MonthlyStatistics stats) { this.stats = stats; }

    @Override protected String reportType() { return "Monthly Statistics Report"; }

    @Override protected String subtitle() {
        return Month.of(stats.month()).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + stats.year();
    }

    @Override protected String fileNameStem() {
        return String.format("monthly_statistics_%04d_%02d", stats.year(), stats.month());
    }

    @Override protected List<ReportSection> sections() {
        if (stats.isEmpty()) {
            return List.of(new ParagraphSection(
                    "No simulation data available for " + subtitle() + ".", true));
        }
        final List<ReportSection> sections = new ArrayList<>();
        sections.add(new ParagraphSection("Summary", true));
        sections.add(new KpiSummarySection(List.of(
                new KpiSummarySection.Kpi("Simulations", String.valueOf(stats.simulationCount())),
                new KpiSummarySection.Kpi("Flights", String.valueOf(stats.flightCount())),
                new KpiSummarySection.Kpi("Violations (" + Math.round(stats.violationRate() * 100) + "%)",
                        String.valueOf(stats.violationCount())),
                new KpiSummarySection.Kpi("Passed", String.valueOf(stats.passedCount())),
                new KpiSummarySection.Kpi("Failed", String.valueOf(stats.failedCount())))));

        sections.add(new ParagraphSection("Flights per Air Control Area", true));
        final Map<String, Number> chartData = new LinkedHashMap<>();
        stats.perArea().forEach((area, s) -> chartData.put(area, s.flights()));
        sections.add(new ChartSection(ChartImageFactory.barChartPng(
                "Flights per Air Control Area", "Area", "Flights", chartData)));

        sections.add(new ParagraphSection("Per-Area Breakdown", true));
        final List<List<String>> rows = new ArrayList<>();
        stats.perArea().forEach((area, s) ->
                rows.add(List.of(area, String.valueOf(s.flights()), String.valueOf(s.violations()))));
        sections.add(new TableSection(List.of("Air Control Area", "Flights", "Violations"), rows));
        return sections;
    }
}
