package flight.dsl.listener;

import flight.dsl.FlightDSLBaseListener;
import flight.dsl.FlightDSLParser;
import flight.dsl.model.FlightStructureSummary;

public class FlightStructureListener extends FlightDSLBaseListener {

    private final FlightStructureSummary summary = new FlightStructureSummary();

    public FlightStructureSummary summary() {
        return summary;
    }

    @Override
    public void enterFlightPlan(final FlightDSLParser.FlightPlanContext ctx) {
        summary.incrementFlightPlanCount();
        summary.addFlightDesignator(ctx.FLIGHT_DESIGNATOR().getText());
    }

    @Override
    public void enterLeg(final FlightDSLParser.LegContext ctx) {
        summary.incrementLegCount();
    }

    @Override
    public void enterClimbSegment(final FlightDSLParser.ClimbSegmentContext ctx) {
        summary.incrementSegmentCount();
    }

    @Override
    public void enterCruiseSegment(final FlightDSLParser.CruiseSegmentContext ctx) {
        summary.incrementSegmentCount();
    }

    @Override
    public void enterDescendSegment(final FlightDSLParser.DescendSegmentContext ctx) {
        summary.incrementSegmentCount();
    }

    @Override
    public void enterProfile(final FlightDSLParser.ProfileContext ctx) {
        summary.incrementProfileCount();
    }
}
