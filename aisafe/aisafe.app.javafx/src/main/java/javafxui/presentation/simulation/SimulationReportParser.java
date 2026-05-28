package javafxui.presentation.simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulationReportParser {

    private static final Pattern TOTAL_FLIGHTS      = Pattern.compile("\"totalFlights\"\\s*:\\s*(\\d+)");
    private static final Pattern TOTAL_VIOLATIONS   = Pattern.compile("\"totalViolationsDetected\"\\s*:\\s*(\\d+)");
    private static final Pattern VALIDATION_RESULT  = Pattern.compile("\"validationResult\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VIOLATION_BLOCK    = Pattern.compile(
            "\"violationId\"\\s*:\\s*(\\d+).*?" +
            "\"timestamp\"\\s*:\\s*([\\d.]+).*?" +
            "\"aircraftA\".*?\"callsign\"\\s*:\\s*\"([^\"]+)\".*?" +
            "\"aircraftB\".*?\"callsign\"\\s*:\\s*\"([^\"]+)\".*?" +
            "\"separationMeters\"\\s*:\\s*([\\d.]+)",
            Pattern.DOTALL);

    public record ViolationEvent(int id, double timestamp, String callsignA, String callsignB, double separationMeters) {}

    public record Report(int totalFlights, int totalViolations, String validationResult, List<ViolationEvent> violations) {}

    public Optional<Report> findAndParse() {
        try {
            final Path simDir = Paths.get("aisafe-simulation").toAbsolutePath();
            Path newest = null;
            try (final var stream = Files.newDirectoryStream(simDir, "simulation_report_*.json")) {
                for (final Path p : stream) {
                    if (newest == null || Files.getLastModifiedTime(p).compareTo(Files.getLastModifiedTime(newest)) > 0) {
                        newest = p;
                    }
                }
            }
            if (newest == null) return Optional.empty();
            return Optional.of(parse(Files.readString(newest)));
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    private Report parse(final String json) {
        final int totalFlights     = matchInt(TOTAL_FLIGHTS, json, 0);
        final int totalViolations  = matchInt(TOTAL_VIOLATIONS, json, 0);
        final String result        = matchStr(VALIDATION_RESULT, json, "UNKNOWN");

        final List<ViolationEvent> violations = new ArrayList<>();
        final Matcher m = VIOLATION_BLOCK.matcher(json);
        while (m.find()) {
            violations.add(new ViolationEvent(
                    Integer.parseInt(m.group(1)),
                    Double.parseDouble(m.group(2)),
                    m.group(3),
                    m.group(4),
                    Double.parseDouble(m.group(5))));
        }
        return new Report(totalFlights, totalViolations, result, violations);
    }

    private int matchInt(final Pattern p, final String s, final int def) {
        final Matcher m = p.matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    private String matchStr(final Pattern p, final String s, final String def) {
        final Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : def;
    }
}
