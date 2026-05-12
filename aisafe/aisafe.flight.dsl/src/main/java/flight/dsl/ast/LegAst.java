package flight.dsl.ast;

import java.util.List;
import java.util.Objects;

public class LegAst {

    private final DepartureAst departure;
    private final ArrivalAst arrival;
    private final FuelAst fuel;
    private final ProfileAst profile;
    private final List<SegmentAst> segments;

    public LegAst(final DepartureAst departure,
                  final ArrivalAst arrival,
                  final FuelAst fuel,
                  final ProfileAst profile,
                  final List<SegmentAst> segments) {
        this.departure = Objects.requireNonNull(departure, "departure");
        this.arrival = Objects.requireNonNull(arrival, "arrival");
        this.fuel = Objects.requireNonNull(fuel, "fuel");
        this.profile = profile;
        this.segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
    }

    public DepartureAst departure() {
        return departure;
    }

    public ArrivalAst arrival() {
        return arrival;
    }

    public FuelAst fuel() {
        return fuel;
    }

    public ProfileAst profile() {
        return profile;
    }

    public List<SegmentAst> segments() {
        return segments;
    }
}

