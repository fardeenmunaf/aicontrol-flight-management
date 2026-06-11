package flightPlan.application;

import aircontrolarea.domain.AirControlArea;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;
import auth.domain.AISafeRoles;
import auth.infrastructure.AISafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flightPlan.domain.AltitudeSlot;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.domain.FlightType;
import flightPlan.domain.Segment;
import flightPlan.repository.FlightPlanRepository;
import node.domain.Coordinates;
import node.domain.Node;
import node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.domain.AirportId;
import shared.domain.FlightPlanId;
import shared.domain.NodeId;
import shared.domain.WeatherDataId;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AddWeatherDataToFlightPlanControllerTest {

    private static final String PILOT    = "weather.pilot@test.com";
    private static final String OTHER    = "other.pilot@test.com";
    private static final String PASSWORD = "Password123";

    // Nodes inside the weather bounding box (41–42 lat, -9 to -8 lon)
    private static final NodeId NODE_START_ID = NodeId.valueOf("node-start");
    private static final NodeId NODE_END_ID   = NodeId.valueOf("node-end");
    private static final Node NODE_START = new Node(NODE_START_ID, "OPO_START", new Coordinates(41.24, -8.68));
    private static final Node NODE_END   = new Node(NODE_END_ID,   "OPO_END",   new Coordinates(41.50, -8.50));

    // Node outside any weather bounding box (France)
    private static final NodeId NODE_FR_START_ID = NodeId.valueOf("node-fr-start");
    private static final NodeId NODE_FR_END_ID   = NodeId.valueOf("node-fr-end");
    private static final Node NODE_FR_START = new Node(NODE_FR_START_ID, "FR_START", new Coordinates(48.85, 2.35));
    private static final Node NODE_FR_END   = new Node(NODE_FR_END_ID,   "FR_END",   new Coordinates(49.00, 2.50));

    private static final AirControlArea AREA = new AirControlArea("PT-WX", 500.0, "Portugal");
    private static final Airport OPO = new Airport(
            "Porto Airport", "Porto", "Portugal", "OPO", "LPPR", 69.0,
            new AirportCoordinates(41.24, -8.68), AREA);
    private static final Airport MAD = new Airport(
            "Madrid Barajas", "Madrid", "Spain", "MAD", "LEMD", 610.0,
            new AirportCoordinates(40.49, -3.57), AREA);

    private final StubFlightPlanRepo planRepo    = new StubFlightPlanRepo();
    private final StubWeatherRepo    weatherRepo = new StubWeatherRepo();
    private final StubAirportRepo    airportRepo = new StubAirportRepo();
    private final StubNodeRepo       nodeRepo    = new StubNodeRepo();

    @BeforeAll
    static void auth() {
        try { AuthzRegistry.configure(new InMemoryUserRepository(), new AISafePasswordPolicy(), new PlainTextEncoder()); } catch (final Exception ignored) {}
        try { AuthzRegistry.userService().registerNewUser(PILOT, PASSWORD, "P", "P", PILOT, Set.of(AISafeRoles.PILOT)); } catch (final Exception ignored) {}
        try { AuthzRegistry.userService().registerNewUser(OTHER, PASSWORD, "O", "O", OTHER, Set.of(AISafeRoles.PILOT)); } catch (final Exception ignored) {}
    }

    @BeforeEach
    void setup() {
        planRepo.clear();
        weatherRepo.clear();
        nodeRepo.clear();
        nodeRepo.save(NODE_START);
        nodeRepo.save(NODE_END);
        nodeRepo.save(NODE_FR_START);
        nodeRepo.save(NODE_FR_END);
        AuthzRegistry.authenticationService().authenticate(PILOT, PASSWORD);
    }

    private AddWeatherDataToFlightPlanController controller() {
        return new AddWeatherDataToFlightPlanController(planRepo, weatherRepo, airportRepo, nodeRepo);
    }

    /** Segment inside the weather bounding box at altitude 0–5000 m. */
    private Segment segmentOverOpo() {
        final Segment seg = new Segment(NODE_START_ID, NODE_END_ID, 270.0, 10.0);
        seg.addAltitudeSlot(new AltitudeSlot(0.0, 5000.0, 500.0));
        return seg;
    }

    /** Segment outside any weather box (France). */
    private Segment segmentOverFrance() {
        final Segment seg = new Segment(NODE_FR_START_ID, NODE_FR_END_ID, 90.0, 5.0);
        seg.addAltitudeSlot(new AltitudeSlot(0.0, 5000.0, 500.0));
        return seg;
    }

    private FlightPlan draftPlan(final Segment... segments) {
        final FlightLeg leg = new FlightLeg(1,
                Instant.parse("2027-05-05T08:00:00Z"),
                Instant.parse("2027-05-05T09:30:00Z"),
                40200.0, AirportId.valueOf("apt-OPO"), AirportId.valueOf("apt-MAD"));
        for (final Segment s : segments) {
            leg.addSegment(s);
        }
        final FlightPlan p = new FlightPlan(FlightPlanId.newId(), "TP1234", FlightType.REGULAR,
                "A320", PILOT, "AA123", "OPO", "MAD", List.of(leg));
        return planRepo.save(p);
    }

    /** Weather overlapping with the leg time, covering OPO coords, altitude 0–10000 m. */
    private WeatherData weatherOverOpo() {
        return weatherRepo.save(new WeatherData("PT-WX",
                41.0, -9.0, 42.0, -8.0,
                0.0, 10000.0,
                270.0, 15.0,
                LocalDate.of(2027, 5, 5),
                LocalTime.of(6, 0), LocalTime.of(12, 0)));
    }

    /** Weather covering OPO coords but at altitude 6000–10000 m (above the segment). */
    private WeatherData weatherHighAltitude() {
        return weatherRepo.save(new WeatherData("PT-WX",
                41.0, -9.0, 42.0, -8.0,
                6000.0, 10000.0,
                180.0, 30.0,
                LocalDate.of(2027, 5, 5),
                LocalTime.of(6, 0), LocalTime.of(12, 0)));
    }

    /** Weather in France — outside the flight path. */
    private WeatherData weatherOverFrance() {
        return weatherRepo.save(new WeatherData("FR-NONE",
                48.0, 2.0, 49.0, 3.0,
                0.0, 10000.0,
                90.0, 10.0,
                LocalDate.of(2027, 5, 5),
                LocalTime.of(6, 0), LocalTime.of(12, 0)));
    }

    /** Weather covering OPO but on the wrong date — no temporal overlap. */
    private WeatherData weatherWrongDate() {
        return weatherRepo.save(new WeatherData("PT-WX",
                41.0, -9.0, 42.0, -8.0,
                0.0, 10000.0,
                180.0, 5.0,
                LocalDate.of(2030, 1, 1),
                LocalTime.of(6, 0), LocalTime.of(12, 0)));
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    void ensureRelevantWeatherIsLinkedToSegment() {
        final Segment seg = segmentOverOpo();
        final FlightPlan plan = draftPlan(seg);
        final WeatherData wd = weatherOverOpo();

        final AddWeatherResult result = controller().addAllRelevantWeatherData(plan);

        assertEquals(1, result.addedCount());
        assertFalse(result.testWasVoided());
        assertTrue(seg.weatherDataIds().contains(wd.identity().toString()));
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureAddingWeatherToTestedPlanVoidsTest() {
        final FlightPlan plan = draftPlan(segmentOverOpo());
        plan.markAsTested();
        planRepo.save(plan);
        weatherOverOpo();

        final AddWeatherResult result = controller().addAllRelevantWeatherData(plan);

        assertTrue(result.addedCount() > 0);
        assertTrue(result.testWasVoided());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureWeatherOutsideSpatialBoxIsNotAdded() {
        final FlightPlan plan = draftPlan(segmentOverFrance());
        weatherOverOpo(); // covers OPO, not France

        final AddWeatherResult result = controller().addAllRelevantWeatherData(plan);

        assertEquals(0, result.addedCount());
    }

    @Test
    void ensureWeatherAboveSegmentAltitudeIsNotAdded() {
        final FlightPlan plan = draftPlan(segmentOverOpo()); // alt 0–5000 m
        weatherHighAltitude(); // covers OPO but at 6000–10000 m

        final AddWeatherResult result = controller().addAllRelevantWeatherData(plan);

        assertEquals(0, result.addedCount());
    }

    @Test
    void ensureWeatherOutsideTimeWindowIsNotAdded() {
        final FlightPlan plan = draftPlan(segmentOverOpo());
        weatherWrongDate();

        final AddWeatherResult result = controller().addAllRelevantWeatherData(plan);

        assertEquals(0, result.addedCount());
    }

    @Test
    void ensureAlreadyAssociatedWeatherIsNotDoubleAdded() {
        final Segment seg = segmentOverOpo();
        final FlightPlan plan = draftPlan(seg);
        weatherOverOpo();

        controller().addAllRelevantWeatherData(plan);
        final int sizeAfterFirst = seg.weatherDataIds().size();

        final AddWeatherResult second = controller().addAllRelevantWeatherData(plan);

        assertEquals(0, second.addedCount());
        assertEquals(sizeAfterFirst, seg.weatherDataIds().size());
    }

    @Test
    void ensureOtherPilotCannotModifyPlan() {
        final FlightPlan plan = draftPlan(segmentOverOpo());
        AuthzRegistry.authenticationService().authenticate(OTHER, PASSWORD);

        assertThrows(SecurityException.class,
                () -> controller().findForWeatherUpdate(plan.identity()));
    }

    @Test
    void ensureVoidPlanIsRejected() {
        final FlightPlan plan = draftPlan(segmentOverOpo());
        plan.markAsTested();
        plan.voidTest();
        planRepo.save(plan);

        assertThrows(IllegalStateException.class,
                () -> controller().findForWeatherUpdate(plan.identity()));
    }

    @Test
    void ensureEditablePlansReturnsOnlyDraftAndTested() {
        final FlightPlan draft = draftPlan(segmentOverOpo());
        final List<FlightPlan> result = new ArrayList<>();
        controller().editablePlansForPilot().forEach(result::add);

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(p -> p.identity().equals(draft.identity())));
    }

    // ── Stubs ──────────────────────────────────────────────────────────────────

    static class StubFlightPlanRepo
            extends InMemoryDomainRepository<FlightPlan, FlightPlanId>
            implements FlightPlanRepository {
        void clear() { data().clear(); }

        @Override public Optional<FlightPlan> findByFlightPlanId(final FlightPlanId id) {
            return matchOne(p -> p.identity().equals(id));
        }
        @Override public Iterable<FlightPlan> findByStatus(final FlightPlanStatus s) {
            return match(p -> p.status() == s);
        }
        @Override public Iterable<FlightPlan> findByRouteIdAndDepartureAfter(final String r, final Instant d) {
            return match(p -> r.equals(p.routeReference()) && p.departureTime() != null && p.departureTime().isAfter(d));
        }
        @Override public Iterable<FlightPlan> findByPilotRef(final String ref) {
            return match(p -> ref.equals(p.pilotRef()));
        }
        @Override public Iterable<FlightPlan> findByFlightDesignator(final String flightDesignator) {
            return match(p -> flightDesignator.equals(p.flightDesignator()));
        }
    }

    static class StubWeatherRepo
            extends InMemoryDomainRepository<WeatherData, WeatherDataId>
            implements WeatherDataRepository {
        void clear() { data().clear(); }

        @Override public Optional<WeatherData> findByAirControlAreaAndDateTime(final String area, final LocalDateTime dt) {
            return Optional.empty();
        }
        @Override public List<WeatherData> findByAirControlArea(final String area) {
            final List<WeatherData> r = new ArrayList<>();
            match(w -> area.equals(w.acaCode())).forEach(r::add);
            return r;
        }
    }

    static class StubAirportRepo
            extends InMemoryDomainRepository<Airport, AirportId>
            implements AirportRepository {
        @Override public Optional<Airport> findByIataCode(final String code) {
            if ("OPO".equalsIgnoreCase(code)) return Optional.of(OPO);
            if ("MAD".equalsIgnoreCase(code)) return Optional.of(MAD);
            return Optional.empty();
        }
        @Override public Optional<Airport> findByIcaoCode(final String code) { return Optional.empty(); }
    }

    static class StubNodeRepo
            extends InMemoryDomainRepository<Node, NodeId>
            implements NodeRepository {
        void clear() { data().clear(); }

        @Override public Optional<Node> findByDesignation(final String designation) {
            return matchOne(n -> designation.equals(n.designation()));
        }
    }
}
