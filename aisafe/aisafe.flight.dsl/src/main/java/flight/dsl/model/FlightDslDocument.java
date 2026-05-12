package flight.dsl.model;

import flight.dsl.ast.FlightPlanAst;

import java.util.List;
import java.util.Objects;

public class FlightDslDocument {

    private final List<FlightPlanAst> flightPlans;
    private final AstLocationRegistry locationRegistry;

    public FlightDslDocument(final List<FlightPlanAst> flightPlans, final AstLocationRegistry locationRegistry) {
        this.flightPlans = List.copyOf(Objects.requireNonNull(flightPlans, "flightPlans"));
        this.locationRegistry = Objects.requireNonNull(locationRegistry, "locationRegistry");
    }

    public List<FlightPlanAst> flightPlans() {
        return flightPlans;
    }

    public AstLocationRegistry locationRegistry() {
        return locationRegistry;
    }
}

