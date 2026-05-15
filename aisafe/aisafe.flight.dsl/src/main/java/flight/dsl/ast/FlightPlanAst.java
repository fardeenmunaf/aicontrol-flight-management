package flight.dsl.ast;

import java.util.List;
import java.util.Objects;

public class FlightPlanAst {

    private final String flightDesignator;
    private final FlightTypeAst type;
    private final String aircraftId;
    private final String routeReference;
    private final String routeOrigin;
    private final String routeDestination;
    private final List<LegAst> legs;

    public FlightPlanAst(final String flightDesignator,
                         final FlightTypeAst type,
                         final String aircraftId,
                         final String routeReference,
                         final String routeOrigin,
                         final String routeDestination,
                         final List<LegAst> legs) {
        this.flightDesignator = Objects.requireNonNull(flightDesignator, "flightDesignator");
        this.type = Objects.requireNonNull(type, "type");
        this.aircraftId = Objects.requireNonNull(aircraftId, "aircraftId");
        this.routeReference = Objects.requireNonNull(routeReference, "routeReference");
        this.routeOrigin = Objects.requireNonNull(routeOrigin, "routeOrigin");
        this.routeDestination = Objects.requireNonNull(routeDestination, "routeDestination");
        this.legs = List.copyOf(Objects.requireNonNull(legs, "legs"));
    }

    public String flightDesignator() {
        return flightDesignator;
    }

    public FlightTypeAst type() {
        return type;
    }

    public String aircraftId() {
        return aircraftId;
    }

    public String routeReference() {
        return routeReference;
    }

    public String routeOrigin() {
        return routeOrigin;
    }

    public String routeDestination() {
        return routeDestination;
    }

    public List<LegAst> legs() {
        return legs;
    }
}
