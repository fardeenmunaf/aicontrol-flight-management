package simulation.application;

import java.time.Instant;
import java.util.List;

/** Parsed result of one simulation run. */
public final class SimulationResultDTO {

    public record FlightOutcome(String callsign, boolean completed, boolean hadViolation) {}
    public record ViolationOutcome(Instant timestamp, simulation.domain.ViolationType violationType, List<String> callsigns) {}

    private final Instant startDateTime;
    private final Instant endDateTime;
    private final List<FlightOutcome> flights;
    private final List<ViolationOutcome> violations;

    public SimulationResultDTO(final Instant startDateTime, final Instant endDateTime,
            final List<FlightOutcome> flights, final List<ViolationOutcome> violations) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.flights = List.copyOf(flights);
        this.violations = List.copyOf(violations);
    }

    public Instant startDateTime() { return startDateTime; }
    public Instant endDateTime() { return endDateTime; }
    public List<FlightOutcome> flights() { return flights; }
    public List<ViolationOutcome> violations() { return violations; }
}
