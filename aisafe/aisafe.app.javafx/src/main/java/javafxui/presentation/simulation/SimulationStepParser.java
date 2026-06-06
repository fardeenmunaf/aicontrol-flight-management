package javafxui.presentation.simulation;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulationStepParser {

    private static final Pattern PHYSICS_ROW = Pattern.compile(
            "^([A-Z0-9]{4,10})\\s+(GATE|CLIMB|CRUISE|DESCEND)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+).*");

    private static final Pattern VIOLATION = Pattern.compile(
            "^\\[([A-Z0-9]+)\\]\\s+SIGUSR1:\\s+safety violation detected\\.");

    private static final Pattern FUEL_EXHAUSTED = Pattern.compile(
            "^\\s*\\*{3}\\s+VIOLATION:\\s+([A-Z0-9]+)\\s+fuel exhausted");

    private static final Pattern COLLISION = Pattern.compile(
            "^\\s*\\*{3}\\s+COLLISION:\\s+([A-Z0-9]+)\\s+<->\\s+([A-Z0-9]+)\\s+dist=");

    private static final Pattern LANDED = Pattern.compile(
            "^\\[([A-Z0-9]+)\\]\\s+Landed\\.");

    private static final Pattern STEP_BANNER = Pattern.compile(
            "t=\\s*[\\d.]+\\s+s\\s+\\[(?<time>\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})\\].*step\\s+(?<step>\\d+)\\s*/\\s*\\d+.*active=\\s*(?<active>\\d+)\\s+pending=\\s*(?<pending>\\d+).*violations=\\s*(?<violations>\\d+)");

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
        if (m.matches()) return Optional.of(m.group(1));
        final Matcher mf = FUEL_EXHAUSTED.matcher(line.trim());
        if (mf.find()) return Optional.of(mf.group(1));
        return Optional.empty();
    }

    /** Matches the collision line and returns the two callsigns involved, or empty. */
    public Optional<String[]> parseCollision(final String line) {
        final Matcher m = COLLISION.matcher(line.trim());
        if (!m.find()) return Optional.empty();
        return Optional.of(new String[] { m.group(1), m.group(2) });
    }

    public Optional<String> parseLanded(final String line) {
        final Matcher m = LANDED.matcher(line.trim());
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
                Integer.parseInt(m.group("violations")),
                m.group("time")));
    }

    public record StepBanner(int step, int active, int pending, int violations, String simTime) {}
}
