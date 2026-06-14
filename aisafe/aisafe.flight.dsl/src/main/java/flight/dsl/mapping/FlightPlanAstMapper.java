package flight.dsl.mapping;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import flight.dsl.ast.AltitudeSlotAst;
import flight.dsl.ast.ClimbEntryAst;
import flight.dsl.ast.CoordinateAst;
import flight.dsl.ast.CruiseEntryAst;
import flight.dsl.ast.DescendEntryAst;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.ProfileAst;
import flight.dsl.ast.SegmentAst;
import flightPlan.domain.AltitudeSlot;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightType;
import flightPlan.domain.ProfileEntry;
import flightPlan.domain.Segment;
import node.domain.Coordinates;
import node.domain.Node;
import node.repository.NodeRepository;
import shared.domain.AirportId;
import shared.domain.FlightPlanId;
import shared.domain.NodeId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Translates a DSL-validated {@link FlightPlanAst} into a {@link FlightPlan} aggregate.
 * Leg endpoints are resolved to {@link AirportId}s; segment endpoints become {@link Node}s,
 * with equal coordinates collapsed into one shared junction node.
 */
public class FlightPlanAstMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AirportRepository airportRepository;
    private final NodeRepository nodeRepository;

    public FlightPlanAstMapper(final AirportRepository airportRepository, final NodeRepository nodeRepository) {
        this.airportRepository = Objects.requireNonNull(airportRepository);
        this.nodeRepository = Objects.requireNonNull(nodeRepository);
    }

    public FlightPlan map(final FlightPlanAst ast) {
        Objects.requireNonNull(ast, "ast");
        final Map<String, NodeId> nodeByCoord = new HashMap<>();
        final List<FlightLeg> legs = new ArrayList<>();
        int sequence = 1;
        for (final LegAst legAst : ast.legs()) {
            legs.add(mapLeg(legAst, sequence++, nodeByCoord));
        }
        return new FlightPlan(
                FlightPlanId.newId(),
                ast.flightDesignator(),
                FlightType.valueOf(ast.type().name()),
                ast.aircraftId(),
                ast.pilotRef(),
                ast.routeReference(),
                ast.routeOrigin(),
                ast.routeDestination(),
                legs);
    }

    private FlightLeg mapLeg(final LegAst legAst, final int sequence, final Map<String, NodeId> nodeByCoord) {
        final Instant departure = toInstant(legAst.departure().date(), legAst.departure().time());
        final Instant arrival = toInstant(legAst.arrival().date(), legAst.arrival().time());
        final AirportId departureAirport = resolveAirport(legAst.departure().airportCode());
        final AirportId arrivalAirport = resolveAirport(legAst.arrival().airportCode());

        final FlightLeg leg = new FlightLeg(sequence, departure, arrival,
                legAst.fuel().quantity(), departureAirport, arrivalAirport);

        final ProfileAst profile = legAst.profile();
        if (profile != null) {
            for (final ClimbEntryAst c : profile.climbEntries()) {
                leg.addProfileEntry(ProfileEntry.climb(c.altitude(), c.altitudeUnit(), c.speed(), c.speedUnit(), c.rate(), c.rateUnit() != null ? c.rateUnit() : "m/s"));
            }
            for (final CruiseEntryAst cr : profile.cruiseEntries()) {
                leg.addProfileEntry(ProfileEntry.cruise(cr.speed(), cr.speedUnit()));
            }
            for (final DescendEntryAst d : profile.descendEntries()) {
                leg.addProfileEntry(ProfileEntry.descend(d.altitude(), d.altitudeUnit(),
                        d.speed(), d.speedUnit(), d.rate(), "m/s"));
            }
        }

        for (final SegmentAst segAst : legAst.segments()) {
            final NodeId from = nodeFor(segAst.from(), nodeByCoord);
            final NodeId to = nodeFor(segAst.to(), nodeByCoord);
            final Segment segment = new Segment(from, to,
                    segAst.wind().direction(), segAst.wind().speed());
            for (final AltitudeSlotAst slot : segAst.altitudeSlots()) {
                segment.addAltitudeSlot(new AltitudeSlot(slot.min(), slot.max(), slot.width()));
            }
            leg.addSegment(segment);
        }
        return leg;
    }

    private NodeId nodeFor(final CoordinateAst c, final Map<String, NodeId> nodeByCoord) {
        final String key = c.latitude() + "," + c.longitude() + "," + c.altitude();
        return nodeByCoord.computeIfAbsent(key, k -> {
            final Node node = nodeRepository.save(new Node(
                    NodeId.newId(), "DSL-" + key, new Coordinates(c.latitude(), c.longitude())));
            return node.identity();
        });
    }

    private AirportId resolveAirport(final String code) {
        return airportRepository.findByIataCode(code)
                .or(() -> airportRepository.findByIcaoCode(code))
                .map(Airport::identity)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found: " + code));
    }

    private static Instant toInstant(final String date, final String time) {
        final LocalDate localDate = LocalDate.parse(date, DATE_FMT);
        final LocalTime localTime = LocalTime.parse(time, TIME_FMT);
        return localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
    }
}
