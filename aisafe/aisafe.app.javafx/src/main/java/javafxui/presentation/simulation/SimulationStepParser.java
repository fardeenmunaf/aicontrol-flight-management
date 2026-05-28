package javafxui.presentation.simulation;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulationStepParser {

    private static final Pattern PHYSICS_ROW = Pattern.compile(
            "^([A-Z0-9]{4,10})\\s+(GATE|CLIMB|CRUISE|DESCEND)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+).*");

    private static final Pattern VIOLATION = Pattern.compile(
            "^\\[([A-Z0-9]+)\\]\\s+SIGUSR1:\\s+safety violation detected\\.");

    private static final Pattern STEP_BANNER = Pattern.compile(
            "t=\\s*[\\d.]+\\s+s.*step\\s+(?<step>\\d+)\\s*/\\s*\\d+.*active=\\s*(?<active>\\d+)\\s+pending=\\s*(?<pending>\\d+).*violations=\\s*(?<violations>\\d+)");

    public Optional<SimStep> parsePhysicsRow(final String line) {
        final Matcher m = PHYSICS_ROW.matcher(line.trim());
        if (!m.matches()) return Optional.empty();

        final String callsign = m.group(1);
        final String phase    = m.group(2);
        final double lat      = Double.parseDouble(m.group(3));
        final double lon      = Double.parseDouble(m.group(4));
        final double alt      = Double.parseDouble(m.group(5));

        final String[] tokens = line.trim().split("\\s+");
        double fuelRemaining  = 0.0;
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (!tokens[i].equals("-")) {
                try {
                    fuelRemaining = Double.parseDouble(tokens[i]);
                    break;
                } catch (final NumberFormatException ignored) {}
            }
        }
        return Optional.of(new SimStep(callsign, phase, lat, lon, alt, fuelRemaining));
    }

    public Optional<String> parseViolation(final String line) {
        final Matcher m = VIOLATION.matcher(line.trim());
        if (!m.matches()) return Optional.empty();
        return Optional.of(m.group(1));
    }

    public Optional<StepBanner> parseStepBanner(final String line) {
        final Matcher m = STEP_BANNER.matcher(line);
        if (!m.find()) return Optional.empty();
        return Optional.of(new StepBanner(
                Integer.parseInt(m.group("step")),
                Integer.parseInt(m.group("active")),
                Integer.parseInt(m.group("pending")),
                Integer.parseInt(m.group("violations"))));
    }

    public record StepBanner(int step, int active, int pending, int violations) {}
}
