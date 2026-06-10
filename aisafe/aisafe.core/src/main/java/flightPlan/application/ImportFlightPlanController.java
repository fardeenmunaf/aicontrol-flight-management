package flightPlan.application;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.FlightPlan;
import flightPlan.repository.FlightPlanRepository;
import node.domain.Coordinates;
import node.domain.Node;
import node.repository.NodeRepository;
import shared.domain.AirportId;
import shared.domain.FlightPlanId;
import shared.domain.NodeId;
import aircraftmodel.repositories.AircraftModelRepository;
import airport.repositories.AirportRepository;
import airport.domain.Airport;

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
    private final AircraftModelRepository aircraftModelRepository;
    private final AirportRepository airportRepository;

    public ImportFlightPlanController(final FlightPlanRepository flightPlanRepository,
            final NodeRepository nodeRepository,
            final AircraftModelRepository aircraftModelRepository,
            final AirportRepository airportRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
        this.nodeRepository = Objects.requireNonNull(nodeRepository);
        this.aircraftModelRepository = Objects.requireNonNull(aircraftModelRepository);
        this.airportRepository = Objects.requireNonNull(airportRepository);
    }

    /**
     * Persists a flight plan imported from a validated DSL file as a JSON document.
     * Segment coordinates are also anchored to new transient Node entities so the
     * topology table stays populated.
     */
    public FlightPlan importFlightPlan(final String aircraftId,
            final String pilotRef, final String routeId, final List<LegImportRequest> legs) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR, AISafeRoles.PILOT);

        final String loggedInEmail = AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated session"));

        if (pilotRef != null && !pilotRef.equalsIgnoreCase(loggedInEmail)) {
            throw new IllegalArgumentException(
                    "Pilot in the DSL file (" + pilotRef + ") does not match the logged-in user (" + loggedInEmail
                            + ")");
        }

        Objects.requireNonNull(aircraftId, "aircraftId");
        if (legs == null || legs.isEmpty()) {
            throw new IllegalArgumentException("At least one leg is required");
        }

        // Validate aircraft
        if (aircraftModelRepository.findByModelName(aircraftId).isEmpty()) {
            throw new IllegalArgumentException("Aircraft model '" + aircraftId + "' not found in the system");
        }

        final FlightPlanId planId = FlightPlanId.newId();
        final String json = buildJson(planId, aircraftId, loggedInEmail, routeId, legs);

        final LegImportRequest firstLeg = legs.get(0);
        final Instant departureTime = toInstant(firstLeg.departureDate, firstLeg.departureTime);

        return flightPlanRepository.save(
                new FlightPlan(planId, json, aircraftId, loggedInEmail, routeId, departureTime));
    }

    private static Instant toInstant(final String date, final String time) {
        final LocalDate localDate = LocalDate.parse(date, DATE_FMT);
        final LocalTime localTime = LocalTime.parse(time, TIME_FMT);
        return localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
    }

    private String buildJson(final FlightPlanId planId, final String aircraftId, final String pilotRef,
            final String routeId, final List<LegImportRequest> legs) {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"flightPlanId\":\"").append(esc(planId.toString())).append("\",");
        sb.append("\"aircraftId\":\"").append(esc(aircraftId)).append("\",");
        sb.append("\"pilotRef\":\"").append(esc(pilotRef)).append("\",");
        sb.append("\"routeReference\":").append(routeId == null ? "null" : "\"" + esc(routeId) + "\"").append(',');
        sb.append("\"legs\":[");
        for (int i = 0; i < legs.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(buildLegJson(legs.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildLegJson(final LegImportRequest leg) {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"sequenceNumber\":").append(leg.sequenceNumber).append(',');
        sb.append("\"departure\":{\"airport\":\"").append(esc(leg.departureAirportCode)).append("\",")
          .append("\"date\":\"").append(esc(leg.departureDate)).append("\",")
          .append("\"time\":\"").append(esc(leg.departureTime)).append("\"},");
        sb.append("\"arrival\":{\"airport\":\"").append(esc(leg.arrivalAirportCode)).append("\",")
          .append("\"date\":\"").append(esc(leg.arrivalDate)).append("\",")
          .append("\"time\":\"").append(esc(leg.arrivalTime)).append("\"},");
        sb.append("\"fuel\":{\"quantity\":").append(leg.fuelQuantity).append(",\"unit\":\"kg\"},");

        // Validate airports exist; anchor segment coordinates to new nodes.
        resolveAirportId(leg.departureAirportCode);
        resolveAirportId(leg.arrivalAirportCode);

        sb.append("\"segments\":[");
        for (int i = 0; i < leg.segments.size(); i++) {
            if (i > 0) sb.append(',');
            final SegmentImportRequest seg = leg.segments.get(i);

            final Node startNode = nodeRepository.save(new Node(
                    NodeId.newId(),
                    "DSL-" + seg.fromLatitude + "," + seg.fromLongitude,
                    new Coordinates(seg.fromLatitude, seg.fromLongitude)));
            final Node endNode = nodeRepository.save(new Node(
                    NodeId.newId(),
                    "DSL-" + seg.toLatitude + "," + seg.toLongitude,
                    new Coordinates(seg.toLatitude, seg.toLongitude)));

            sb.append("{\"from\":{\"nodeId\":\"").append(esc(startNode.identity().toString())).append("\",")
              .append("\"latitude\":").append(seg.fromLatitude).append(',')
              .append("\"longitude\":").append(seg.fromLongitude).append("},")
              .append("\"to\":{\"nodeId\":\"").append(esc(endNode.identity().toString())).append("\",")
              .append("\"latitude\":").append(seg.toLatitude).append(',')
              .append("\"longitude\":").append(seg.toLongitude).append("}}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private AirportId resolveAirportId(final String code) {
        return airportRepository.findByIataCode(code)
                .or(() -> airportRepository.findByIcaoCode(code))
                .map(Airport::identity)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found: " + code));
    }

    private static String esc(final String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
