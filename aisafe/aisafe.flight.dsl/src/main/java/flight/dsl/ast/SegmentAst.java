package flight.dsl.ast;

import java.util.Objects;

public class SegmentAst {

    private final SegmentModeAst mode;
    private final CoordinateAst from;
    private final CoordinateAst to;

    public SegmentAst(final SegmentModeAst mode, final CoordinateAst from, final CoordinateAst to) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    public SegmentModeAst mode() {
        return mode;
    }

    public CoordinateAst from() {
        return from;
    }

    public CoordinateAst to() {
        return to;
    }
}

