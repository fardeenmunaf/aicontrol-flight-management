package flight.dsl.visitor;

import flight.dsl.FlightDSLBaseVisitor;
import flight.dsl.FlightDSLParser;
import flight.dsl.ast.AltitudeSlotAst;
import flight.dsl.ast.ArrivalAst;
import flight.dsl.ast.ClimbEntryAst;
import flight.dsl.ast.CoordinateAst;
import flight.dsl.ast.DepartureAst;
import flight.dsl.ast.DescendEntryAst;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.FlightTypeAst;
import flight.dsl.ast.FuelAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.ProfileAst;
import flight.dsl.ast.ProfileEntryAst;
import flight.dsl.ast.SegmentAst;
import flight.dsl.ast.SegmentModeAst;
import flight.dsl.ast.WindAst;
import flight.dsl.model.AstLocationRegistry;
import flight.dsl.model.FlightDslDocument;

import java.util.ArrayList;
import java.util.List;

public class FlightAstBuilderVisitor extends FlightDSLBaseVisitor<Object> {

    private final AstLocationRegistry locationRegistry = new AstLocationRegistry();

    @Override
    public Object visitStart(final FlightDSLParser.StartContext ctx) {
        final List<FlightPlanAst> flightPlans = new ArrayList<>();
        for (final FlightDSLParser.FlightPlanContext flightPlanContext : ctx.flightPlan()) {
            flightPlans.add((FlightPlanAst) visitFlightPlan(flightPlanContext));
        }
        return new FlightDslDocument(flightPlans, locationRegistry);
    }

    @Override
    public Object visitFlightPlan(final FlightDSLParser.FlightPlanContext ctx) {
        final String flightDesignator = ctx.FLIGHT_DESIGNATOR().getText();
        final String aircraftId = ctx.aircraftId().getText();
        final String routeReference = ctx.routeRef().getText();
        final String routeOrigin = ctx.airportCode(0).getText();
        final String routeDestination = ctx.airportCode(1).getText();
        final FlightTypeAst type = (FlightTypeAst) visitFlightType(ctx.flightType());
        final List<LegAst> legs = new ArrayList<>();
        for (final FlightDSLParser.LegContext legContext : ctx.leg()) {
            legs.add((LegAst) visitLeg(legContext));
        }

        final FlightPlanAst flightPlanAst = new FlightPlanAst(flightDesignator, type, aircraftId, routeReference, routeOrigin, routeDestination, legs);
        register(flightPlanAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return flightPlanAst;
    }

    @Override
    public Object visitFlightType(final FlightDSLParser.FlightTypeContext ctx) {
        if (ctx.REGULAR() != null) {
            return FlightTypeAst.REGULAR;
        }
        return FlightTypeAst.CHARTER;
    }

    @Override
    public Object visitLeg(final FlightDSLParser.LegContext ctx) {
        final DepartureAst departure = (DepartureAst) visitDeparture(ctx.departure());
        final ArrivalAst arrival = (ArrivalAst) visitArrival(ctx.arrival());
        final FuelAst fuel = (FuelAst) visitFuel(ctx.fuel());
        final ProfileAst profile = ctx.profile() == null ? null : (ProfileAst) visitProfile(ctx.profile());
        final List<SegmentAst> segments = new ArrayList<>();
        segments.add((SegmentAst) visitClimbSegment(ctx.climbSegment()));
        segments.add((SegmentAst) visitCruiseSegment(ctx.cruiseSegment()));
        segments.add((SegmentAst) visitDescendSegment(ctx.descendSegment()));

        final LegAst legAst = new LegAst(departure, arrival, fuel, profile, segments);
        register(legAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return legAst;
    }

    @Override
    public Object visitDeparture(final FlightDSLParser.DepartureContext ctx) {
        final DepartureAst departureAst = new DepartureAst(
                ctx.airportCode().getText(),
                ctx.DATE_LITERAL().getText(),
                ctx.TIME_LITERAL().getText()
        );
        register(departureAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return departureAst;
    }

    @Override
    public Object visitArrival(final FlightDSLParser.ArrivalContext ctx) {
        final ArrivalAst arrivalAst = new ArrivalAst(
                ctx.airportCode().getText(),
                ctx.DATE_LITERAL().getText(),
                ctx.TIME_LITERAL().getText()
        );
        register(arrivalAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return arrivalAst;
    }

    @Override
    public Object visitFuel(final FlightDSLParser.FuelContext ctx) {
        final FuelAst fuelAst = new FuelAst(
                Double.parseDouble(ctx.positiveNumber().getText()),
                ctx.unit().getText()
        );
        register(fuelAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return fuelAst;
    }

    @Override
    public Object visitProfile(final FlightDSLParser.ProfileContext ctx) {
        final List<ClimbEntryAst> climbEntries = new ArrayList<>();
        final List<DescendEntryAst> descendEntries = new ArrayList<>();
        for (final FlightDSLParser.ProfileEntryContext entryContext : ctx.profileEntry()) {
            final ProfileEntryAst entry = (ProfileEntryAst) visitProfileEntry(entryContext);
            if (entry instanceof ClimbEntryAst climbEntryAst) {
                climbEntries.add(climbEntryAst);
            } else if (entry instanceof DescendEntryAst descendEntryAst) {
                descendEntries.add(descendEntryAst);
            }
        }

        final ProfileAst profileAst = new ProfileAst(climbEntries, descendEntries);
        register(profileAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return profileAst;
    }

    @Override
    public Object visitProfileEntry(final FlightDSLParser.ProfileEntryContext ctx) {
        if (ctx.climbEntry() != null) {
            return visitClimbEntry(ctx.climbEntry());
        }
        return visitDescendEntry(ctx.descendEntry());
    }

    @Override
    public Object visitClimbEntry(final FlightDSLParser.ClimbEntryContext ctx) {
        final ClimbEntryAst climbEntryAst = new ClimbEntryAst(
                Double.parseDouble(ctx.positiveNumber(0).getText()),
                ctx.unit(0).getText(),
                Double.parseDouble(ctx.positiveNumber(1).getText()),
                ctx.unit(1).getText()
        );
        register(climbEntryAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return climbEntryAst;
    }

    @Override
    public Object visitDescendEntry(final FlightDSLParser.DescendEntryContext ctx) {
        final DescendEntryAst descendEntryAst = new DescendEntryAst(
                Double.parseDouble(ctx.positiveNumber(0).getText()),
                ctx.unit(0).getText(),
                Double.parseDouble(ctx.positiveNumber(1).getText()),
                ctx.unit(1).getText(),
                Double.parseDouble(ctx.signedNumber().getText())
        );
        register(descendEntryAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return descendEntryAst;
    }

    @Override
    public Object visitClimbSegment(final FlightDSLParser.ClimbSegmentContext ctx) {
        return buildSegmentAst(ctx.coord(0), ctx.coord(1), ctx.altitudeSlot(), ctx.wind(), SegmentModeAst.CLIMB, ctx.getStart());
    }

    @Override
    public Object visitCruiseSegment(final FlightDSLParser.CruiseSegmentContext ctx) {
        return buildSegmentAst(ctx.coord(0), ctx.coord(1), ctx.altitudeSlot(), ctx.wind(), SegmentModeAst.CRUISE, ctx.getStart());
    }

    @Override
    public Object visitDescendSegment(final FlightDSLParser.DescendSegmentContext ctx) {
        return buildSegmentAst(ctx.coord(0), ctx.coord(1), ctx.altitudeSlot(), ctx.wind(), SegmentModeAst.DESCEND, ctx.getStart());
    }

    private SegmentAst buildSegmentAst(
            final FlightDSLParser.CoordContext fromCtx,
            final FlightDSLParser.CoordContext toCtx,
            final java.util.List<FlightDSLParser.AltitudeSlotContext> slotCtxList,
            final FlightDSLParser.WindContext windCtx,
            final SegmentModeAst mode,
            final org.antlr.v4.runtime.Token startToken) {
        final CoordinateAst from = (CoordinateAst) visitCoord(fromCtx);
        final CoordinateAst to = (CoordinateAst) visitCoord(toCtx);
        final List<AltitudeSlotAst> slots = new ArrayList<>();
        for (final FlightDSLParser.AltitudeSlotContext slotCtx : slotCtxList) {
            slots.add((AltitudeSlotAst) visitAltitudeSlot(slotCtx));
        }
        final WindAst wind = (WindAst) visitWind(windCtx);
        final SegmentAst segmentAst = new SegmentAst(from, to, slots, wind, mode);
        register(segmentAst, startToken.getLine(), startToken.getCharPositionInLine());
        return segmentAst;
    }

    @Override
    public Object visitAltitudeSlot(final FlightDSLParser.AltitudeSlotContext ctx) {
        final AltitudeSlotAst slotAst = new AltitudeSlotAst(
                Double.parseDouble(ctx.positiveNumber(0).getText()),
                ctx.unit(0).getText(),
                Double.parseDouble(ctx.positiveNumber(1).getText()),
                ctx.unit(1).getText(),
                Double.parseDouble(ctx.positiveNumber(2).getText()),
                ctx.unit(2).getText()
        );
        register(slotAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return slotAst;
    }

    @Override
    public Object visitWind(final FlightDSLParser.WindContext ctx) {
        final WindAst windAst = new WindAst(
                Double.parseDouble(ctx.positiveNumber(0).getText()),
                Double.parseDouble(ctx.positiveNumber(1).getText())
        );
        register(windAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return windAst;
    }

    @Override
    public Object visitCoord(final FlightDSLParser.CoordContext ctx) {
        final CoordinateAst coordinateAst = new CoordinateAst(
                Double.parseDouble(ctx.signedNumber(0).getText()),
                Double.parseDouble(ctx.signedNumber(1).getText()),
                Double.parseDouble(ctx.positiveNumber().getText()),
                ctx.unit().getText()
        );
        register(coordinateAst, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        return coordinateAst;
    }

    public AstLocationRegistry locationRegistry() {
        return locationRegistry;
    }

    private void register(final Object node, final int line, final int column) {
        locationRegistry.register(node, line, column);
    }
}
