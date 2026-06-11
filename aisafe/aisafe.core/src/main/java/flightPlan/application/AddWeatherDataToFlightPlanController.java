package flightPlan.application;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightPlan.domain.AltitudeSlot;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.domain.Segment;
import flightPlan.repository.FlightPlanRepository;
import node.domain.Node;
import node.repository.NodeRepository;
import shared.domain.FlightPlanId;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AddWeatherDataToFlightPlanController {

    private final FlightPlanRepository flightPlanRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final AirportRepository airportRepository;
    private final NodeRepository nodeRepository;

    public AddWeatherDataToFlightPlanController(
            final FlightPlanRepository flightPlanRepository,
            final WeatherDataRepository weatherDataRepository,
            final AirportRepository airportRepository,
            final NodeRepository nodeRepository) {
        this.flightPlanRepository = Objects.requireNonNull(flightPlanRepository);
        this.weatherDataRepository = Objects.requireNonNull(weatherDataRepository);
        this.airportRepository = Objects.requireNonNull(airportRepository);
        this.nodeRepository = Objects.requireNonNull(nodeRepository);
    }

    /** Returns DRAFT and TESTED flight plans owned by the logged-in pilot. */
    public Iterable<FlightPlan> editablePlansForPilot() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final String email = loggedInEmail();
        final List<FlightPlan> result = new ArrayList<>();
        flightPlanRepository.findByPilotRef(email).forEach(p -> {
            if (p.status() == FlightPlanStatus.DRAFT || p.status() == FlightPlanStatus.TESTED) {
                result.add(p);
            }
        });
        return result;
    }

    /** Finds a flight plan by ID, verifying ownership and editability (DRAFT or TESTED). */
    public FlightPlan findForWeatherUpdate(final FlightPlanId planId) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        final FlightPlan plan = flightPlanRepository.findByFlightPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Flight plan not found: " + planId));
        if (!plan.pilotRef().equals(loggedInEmail())) {
            throw new SecurityException("You are not authorized to modify this flight plan.");
        }
        if (plan.status() != FlightPlanStatus.DRAFT && plan.status() != FlightPlanStatus.TESTED) {
            throw new IllegalStateException("Weather data cannot be added to a flight plan in its current state.");
        }
        return plan;
    }

    /**
     * Automatically finds and associates weather records directly with each flight segment.
     *
     * For every (segment, weatherData) pair where:
     *   1. Temporal  — the weather window overlaps with the leg's departure–arrival window
     *   2. Spatial   — at least one segment node coordinate is inside the weather bounding box
     *   3. Altitude  — the segment's altitude slot overlaps with [altitudeInf, altitudeSup]
     *
     * the weather record is stored in SegmentWeatherData (one row per segment–weather pair).
     * The planned wind (Segment.windSpeed/windDirection) represents what was planned;
     * the associated WeatherData represents the actual observed wind at that position and time.
     *
     * @return result carrying whether the test was voided and total number of associations made.
     */
    public AddWeatherResult addAllRelevantWeatherData(final FlightPlan plan) {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);

        boolean testWasVoided = false;
        int added = 0;

        for (final WeatherData wd : weatherDataRepository.findAll()) {
            final Instant wdStart = LocalDateTime.of(wd.date(), wd.startHour()).toInstant(ZoneOffset.UTC);
            final Instant wdEnd   = LocalDateTime.of(wd.date(), wd.endHour()).toInstant(ZoneOffset.UTC);

            for (final FlightLeg leg : plan.legs()) {
                // 1. Temporal overlap
                if (!leg.departureTime().isBefore(wdEnd) || !leg.arrivalTime().isAfter(wdStart)) {
                    continue;
                }

                for (final Segment seg : leg.segments()) {
                    // Skip already associated
                    if (seg.weatherDataIds().contains(wd.identity().toString())) {
                        continue;
                    }

                    // 2. Altitude overlap
                    if (!altitudeOverlaps(seg, wd)) {
                        continue;
                    }

                    // 3. Spatial: at least one endpoint inside the bounding box
                    final Optional<Node> start = nodeRepository.ofIdentity(seg.startNodeId());
                    final Optional<Node> end   = nodeRepository.ofIdentity(seg.endNodeId());

                    final boolean inBox =
                            start.map(n -> inBoundingBox(n.coordinates().latitude(), n.coordinates().longitude(), wd)).orElse(false)
                         || end.map(n   -> inBoundingBox(n.coordinates().latitude(), n.coordinates().longitude(), wd)).orElse(false);

                    if (inBox) {
                        final boolean voided = plan.addWeatherDataToSegment(seg, wd.identity());
                        testWasVoided = testWasVoided || voided;
                        added++;
                    }
                }
            }
        }

        if (added > 0) {
            flightPlanRepository.save(plan);
        }

        return new AddWeatherResult(testWasVoided, added);
    }

    // ── Altitude overlap ───────────────────────────────────────────────────────

    private static boolean altitudeOverlaps(final Segment seg, final WeatherData wd) {
        if (seg.altitudeSlots().isEmpty()) return true; // no altitude info → lenient
        final double segMin = seg.altitudeSlots().stream()
                .mapToDouble(AltitudeSlot::minimumAltitude).min().orElse(0);
        final double segMax = seg.altitudeSlots().stream()
                .mapToDouble(AltitudeSlot::maximumAltitude).max().orElse(Double.MAX_VALUE);
        return segMin <= wd.altitudeSup() && segMax >= wd.altitudeInf();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Optional<Airport> lookupAirport(final String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return airportRepository.findByIataCode(code)
                .or(() -> airportRepository.findByIcaoCode(code));
    }

    private static boolean inBoundingBox(final double lat, final double lon, final WeatherData wd) {
        return lat >= Math.min(wd.latitude1(), wd.latitude2())
            && lat <= Math.max(wd.latitude1(), wd.latitude2())
            && lon >= Math.min(wd.longitude1(), wd.longitude2())
            && lon <= Math.max(wd.longitude1(), wd.longitude2());
    }

    private static String loggedInEmail() {
        return AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated session"));
    }
}
