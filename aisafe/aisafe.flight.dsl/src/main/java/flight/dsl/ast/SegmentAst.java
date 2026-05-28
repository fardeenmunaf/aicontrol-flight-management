package flight.dsl.ast;

import java.util.List;
import java.util.Objects;

public class SegmentAst {

    private final CoordinateAst from;
    private final CoordinateAst to;
    private final List<AltitudeSlotAst> altitudeSlots;
    private final WindAst wind;
    private final SegmentModeAst mode;

    public SegmentAst(final CoordinateAst from,
                      final CoordinateAst to,
                      final List<AltitudeSlotAst> altitudeSlots,
                      final WindAst wind,
                      final SegmentModeAst mode) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.altitudeSlots = List.copyOf(Objects.requireNonNull(altitudeSlots, "altitudeSlots"));
        this.wind = Objects.requireNonNull(wind, "wind");
        this.mode = mode;
    }

    public CoordinateAst from() { return from; }
    public CoordinateAst to() { return to; }
    public List<AltitudeSlotAst> altitudeSlots() { return altitudeSlots; }
    public WindAst wind() { return wind; }

    public SegmentModeAst mode() { return mode; }
}
