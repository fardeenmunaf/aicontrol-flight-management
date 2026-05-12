package flight.dsl.ast;

import java.util.List;
import java.util.Objects;

public class FlightPlanAst {

    private final String flightDesignator;
    private final FlightTypeAst type;
    private final String routeReference;
    private final List<LegAst> legs;

    public FlightPlanAst(final String flightDesignator,
                         final FlightTypeAst type,
                         final String routeReference,
                         final List<LegAst> legs) {
        this.flightDesignator = Objects.requireNonNull(flightDesignator, "flightDesignator");
        this.type = Objects.requireNonNull(type, "type");
        this.routeReference = Objects.requireNonNull(routeReference, "routeReference");
        this.legs = List.copyOf(Objects.requireNonNull(legs, "legs"));
    }

    public String flightDesignator() {
        return flightDesignator;
    }

    public FlightTypeAst type() {
        return type;
    }

    public String routeReference() {
        return routeReference;
    }

    public List<LegAst> legs() {
        return legs;
    }
}

