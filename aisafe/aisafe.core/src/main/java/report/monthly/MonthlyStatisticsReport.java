package report.monthly;

import report.framework.Report;
import report.framework.ReportSection;
import report.framework.render.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

public class MonthlyStatisticsReport extends Report {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

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

        // ── Safety Violations Detail ──────────────────────────────────────────────
        sections.add(new ParagraphSection("Safety Violations Detail", true));
        final List<List<String>> vRows = new ArrayList<>();
        if (stats.violationDetails().isEmpty()) {
            vRows.add(List.of("—", "No violations recorded this month.", "—", "—", "—"));
        } else {
            for (final MonthlyStatistics.ViolationDetail d : stats.violationDetails()) {
                vRows.add(List.of(
                        TS_FMT.format(d.timestamp()),
                        d.flightCallsign(),
                        formatType(d.violationType()),
                        d.involvedCallsigns(),
                        d.area()));
            }
        }
        sections.add(new TableSection(
                List.of("Timestamp (UTC)", "Flight", "Type", "Involved Aircraft", "Area"),
                vRows));

        return sections;
    }

    private static String formatType(final String raw) {
        if (raw == null || raw.isBlank() || "—".equals(raw)) return "—";
        // Convert SNAKE_CASE enum name to "Title Case" for readability.
        return Arrays.stream(raw.split("_"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(raw);
    }
}
