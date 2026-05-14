package flightPlan.application;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.Segment;
import flightPlan.repository.FlightPlanRepository;
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
import java.util.List;
import java.util.Objects;

public class ImportFlightPlanController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final FlightPlanRepository flightPlanRepository;
    private final NodeRepository nodeRepository;

    public ImportFlightPlanController(final FlightPlanRepository flightPlanRepository,
                                      final NodeRepository nodeRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
        this.nodeRepository = Objects.requireNonNull(nodeRepository);
    }

    /**
     * Persists a flight plan imported from a validated DSL file.
     * Segments are anchored to new transient Node entities created from the DSL coordinates.
     */
    public FlightPlan importFlightPlan(final String designator, final List<LegImportRequest> legs) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR, AISafeRoles.PILOT);

        Objects.requireNonNull(designator, "designator");
        if (legs == null || legs.isEmpty()) {
            throw new IllegalArgumentException("At least one leg is required");
        }

        final FlightPlanId planId = FlightPlanId.valueOf(designator);
        if (flightPlanRepository.findByFlightPlanId(planId).isPresent()) {
            throw new IllegalArgumentException("Flight plan '" + designator + "' already exists in the system");
        }

        final double totalFuel = legs.stream().mapToDouble(l -> l.fuelQuantity).sum();
        final LegImportRequest firstLeg = legs.get(0);
        final Instant departureDateTime = toInstant(firstLeg.departureDate, firstLeg.departureTime);

        final FlightPlan plan = new FlightPlan(planId, departureDateTime, totalFuel, 0, 0.0);

        for (final LegImportRequest legReq : legs) {
            final Instant legDeparture = toInstant(legReq.departureDate, legReq.departureTime);
            final Instant legArrival = toInstant(legReq.arrivalDate, legReq.arrivalTime);

            final FlightLeg leg = new FlightLeg(
                    legReq.sequenceNumber,
                    legDeparture,
                    legArrival,
                    legReq.fuelQuantity,
                    AirportId.valueOf(legReq.departureAirportCode),
                    AirportId.valueOf(legReq.arrivalAirportCode));

            for (final SegmentImportRequest segReq : legReq.segments) {
                final Node startNode = nodeRepository.save(new Node(
                        NodeId.newId(),
                        "DSL-" + segReq.fromLatitude + "," + segReq.fromLongitude,
                        new Coordinates(segReq.fromLatitude, segReq.fromLongitude)));

                final Node endNode = nodeRepository.save(new Node(
                        NodeId.newId(),
                        "DSL-" + segReq.toLatitude + "," + segReq.toLongitude,
                        new Coordinates(segReq.toLatitude, segReq.toLongitude)));

                leg.addSegment(new Segment(startNode.identity(), endNode.identity(), 0.0, 0.0));
            }

            plan.addLeg(leg);
        }

        return flightPlanRepository.save(plan);
    }

    private static Instant toInstant(final String date, final String time) {
        final LocalDate localDate = LocalDate.parse(date, DATE_FMT);
        final LocalTime localTime = LocalTime.parse(time, TIME_FMT);
        return localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
    }
}
