package pilot.server;

import aircraft.domain.Aircraft;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import flight.dsl.mapping.CreationResult;
import flight.dsl.mapping.FlightPlanCreationService;
import flight.dsl.mapping.ValidateFlightPlanService;
import flightPlan.application.FlightPlanValidationResult;
import flightPlan.application.ValidateFlightPlanController;
import flightPlan.domain.AltitudeSlot;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.Segment;
import infrastructure.persistence.PersistenceContext;
import node.domain.Node;
import pilot.domain.Pilot;
import pilot.domain.PilotCompanyAssignment;
import remotelogging.domain.RemoteAccessEventType;
import remotelogging.domain.RemoteService;
import remotelogging.udp.UdpLogSender;
import shared.domain.UserId;
import weatherData.domain.WeatherData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public class AttendPilotClient implements Runnable {

    private final Socket cliSock;
    private SystemUser authenticatedUser;

    public AttendPilotClient(final Socket s) {
        this.cliSock = s;
    }

    @Override
    public void run() {
        final String clientIp   = cliSock.getInetAddress().getHostAddress();
        final int    clientPort = cliSock.getPort();
        try (cliSock;
             DataInputStream  sIn  = new DataInputStream(cliSock.getInputStream());
             DataOutputStream sOut = new DataOutputStream(cliSock.getOutputStream())) {

            System.out.println("[PilotServer] Client connected from " + clientIp + ":" + clientPort);

            boolean running = true;
            while (running) {
                final PilotProtocolMessage msg = PilotProtocolCodec.read(sIn);
                switch (msg.code()) {
                    case COMMTEST      -> PilotProtocolCodec.write(sOut, ack("OK"));
                    case AUTH          -> handleAuth(msg, sOut, clientIp, clientPort);
                    case LIST_MY_PLANS -> handleListMyPlans(sOut);
                    case CREATE_PLAN   -> handleCreatePlan(msg, sOut);
                    case VALIDATE_PLAN -> handleValidatePlan(msg, sOut);
                    case ADD_WEATHER   -> handleAddWeather(msg, sOut);
                    case LIST_FLEET    -> handleListFleet(sOut);
                    case LIST_ROUTES   -> handleListRoutes(sOut);
                    case DISCONN       -> {
                        PilotProtocolCodec.write(sOut, ack("BYE"));
                        running = false;
                        if (authenticatedUser != null) {
                            UdpLogSender.instance().log(
                                    authenticatedUser.username().toString(),
                                    clientIp, clientPort,
                                    RemoteService.US086, RemoteAccessEventType.LOGOUT);
                        }
                    }
                    default -> PilotProtocolCodec.write(sOut, err("Unsupported operation"));
                }
            }
        } catch (final IOException e) {
            System.err.println("[PilotServer] Connection ended: " + e.getMessage());
            if (authenticatedUser != null) {
                UdpLogSender.instance().log(
                        authenticatedUser.username().toString(),
                        clientIp, clientPort,
                        RemoteService.US086, RemoteAccessEventType.DISCONNECT);
            }
        }
    }

    private void handleAuth(final PilotProtocolMessage msg, final DataOutputStream sOut,
                            final String ip, final int port) throws IOException {
        final String[] parts = msg.dataAsString().split("\0");
        if (parts.length != 2) {
            PilotProtocolCodec.write(sOut, err("Invalid auth format"));
            return;
        }
        final String user = parts[0];
        final String pass = parts[1];

        final Optional<UserSession> session = AuthzRegistry.authenticationService().authenticate(user, pass);
        if (session.isPresent()) {
            this.authenticatedUser = session.get().authenticatedUser();
            if (this.authenticatedUser.roleTypes().contains(AISafeRoles.PILOT)) {
                UdpLogSender.instance().log(user, ip, port, RemoteService.US086, RemoteAccessEventType.SUCCESSFUL_LOGIN);
                PilotProtocolCodec.write(sOut, ack("Authenticated as Pilot"));
            } else {
                UdpLogSender.instance().log(user, ip, port, RemoteService.US086, RemoteAccessEventType.FAILED_LOGIN);
                this.authenticatedUser = null;
                PilotProtocolCodec.write(sOut, err("Not authorized: Pilot role required"));
            }
        } else {
            UdpLogSender.instance().log(user, ip, port, RemoteService.US086, RemoteAccessEventType.FAILED_LOGIN);
            PilotProtocolCodec.write(sOut, err("Invalid credentials"));
        }
    }

    private void handleListMyPlans(final DataOutputStream sOut) throws IOException {
        if (!isAuthenticated(sOut)) return;
        try {
            final String pilotEmail = authenticatedUser.identity().toString();
            final Iterable<FlightPlan> plans = PersistenceContext.repositories().flightPlans()
                    .findByPilotRef(pilotEmail);
            final StringJoiner response = new StringJoiner("\n");
            for (final FlightPlan p : plans) {
                response.add(p.flightDesignator() + ";"
                        + p.status() + ";"
                        + p.routeOrigin() + ";"
                        + p.routeDestination() + ";"
                        + p.departureTime() + ";"
                        + p.identity().toString());
            }
            PilotProtocolCodec.write(sOut, ack(response.toString()));
        } catch (final Throwable t) {
            System.err.println("[PilotServer] handleListMyPlans error: " + t);
            PilotProtocolCodec.write(sOut, err("Error listing plans: " + t.getMessage()));
        }
    }

    private void handleCreatePlan(final PilotProtocolMessage msg, final DataOutputStream sOut) throws IOException {
        if (!isAuthenticated(sOut)) return;
        try {
            final String dslText = msg.dataAsString();
            if (dslText == null || dslText.isBlank()) {
                PilotProtocolCodec.write(sOut, err("DSL text is required"));
                return;
            }
            final FlightPlanCreationService service = new FlightPlanCreationService(
                    PersistenceContext.repositories().flightPlans(),
                    PersistenceContext.repositories().airports(),
                    PersistenceContext.repositories().nodes(),
                    PersistenceContext.repositories().aircraft());
            final CreationResult result = service.createFromText(dslText);
            final StringBuilder sb = new StringBuilder();
            if (result.hasErrors()) {
                sb.append("ERRORS:\n");
                result.errors().forEach(e -> sb.append("  - ").append(e).append("\n"));
            }
            if (!result.created().isEmpty()) {
                sb.append("Created ").append(result.created().size()).append(" flight plan(s):\n");
                result.created().forEach(p -> sb.append("  + ").append(p.flightDesignator()).append("\n"));
            }
            PilotProtocolCodec.write(sOut, ack(sb.toString().trim()));
        } catch (final Throwable t) {
            System.err.println("[PilotServer] handleCreatePlan error: " + t);
            PilotProtocolCodec.write(sOut, err("Error creating plan: " + t.getMessage()));
        }
    }

    private void handleValidatePlan(final PilotProtocolMessage msg, final DataOutputStream sOut) throws IOException {
        if (!isAuthenticated(sOut)) return;
        try {
            final String planId = msg.dataAsString().trim();
            if (planId.isEmpty()) {
                PilotProtocolCodec.write(sOut, err("Flight plan ID is required"));
                return;
            }
            final String pilotEmail = authenticatedUser.identity().toString();
            final List<FlightPlan> plans = new ArrayList<>();
            PersistenceContext.repositories().flightPlans()
                    .findByPilotRef(pilotEmail)
                    .forEach(plans::add);

            final Optional<FlightPlan> opt = plans.stream()
                    .filter(p -> p.identity().toString().equals(planId))
                    .findFirst();
            if (opt.isEmpty()) {
                PilotProtocolCodec.write(sOut, err("Flight plan not found or not yours: " + planId));
                return;
            }

            final ValidateFlightPlanService service = new ValidateFlightPlanService(
                    new ValidateFlightPlanController(PersistenceContext.repositories().flightPlans()),
                    PersistenceContext.repositories().airports());

            final FlightPlanValidationResult result = service.validate(opt.get().identity());
            final StringBuilder sb = new StringBuilder();
            if (!result.dslErrors().isEmpty()) {
                sb.append("DSL Validation FAILED:\n");
                result.dslErrors().forEach(e -> sb.append("  [DSL] ").append(e).append("\n"));
            } else {
                sb.append("DSL Validation: PASSED\n");
            }
            if (result.physicalCheckRan()) {
                if (!result.physicalFailures().isEmpty()) {
                    sb.append("Physical Validation FAILED:\n");
                    result.physicalFailures().forEach(f -> sb.append("  [PHYSICAL] ").append(f).append("\n"));
                } else {
                    sb.append("Physical Validation: PASSED\n");
                }
            }
            sb.append(result.passed()
                    ? "RESULT: PASSED - Flight plan is now TESTED."
                    : "RESULT: FAILED - Flight plan remains DRAFT.");
            PilotProtocolCodec.write(sOut, ack(sb.toString().trim()));
        } catch (final Throwable t) {
            System.err.println("[PilotServer] handleValidatePlan error: " + t);
            PilotProtocolCodec.write(sOut, err("Error validating plan: " + t.getMessage()));
        }
    }

    private void handleAddWeather(final PilotProtocolMessage msg, final DataOutputStream sOut) throws IOException {
        if (!isAuthenticated(sOut)) return;
        try {
            final String planId = msg.dataAsString().trim();
            if (planId.isEmpty()) {
                PilotProtocolCodec.write(sOut, err("Flight plan ID is required"));
                return;
            }
            final String pilotEmail = authenticatedUser.identity().toString();
            final List<FlightPlan> plans = new ArrayList<>();
            PersistenceContext.repositories().flightPlans()
                    .findByPilotRef(pilotEmail)
                    .forEach(plans::add);

            final Optional<FlightPlan> opt = plans.stream()
                    .filter(p -> p.identity().toString().equals(planId))
                    .findFirst();
            if (opt.isEmpty()) {
                PilotProtocolCodec.write(sOut, err("Flight plan not found or not yours: " + planId));
                return;
            }

            final FlightPlan plan = opt.get();
            boolean testWasVoided = false;
            int added = 0;

            for (final WeatherData wd : PersistenceContext.repositories().weatherData().findAll()) {
                final Instant wdStart = LocalDateTime.of(wd.date(), wd.startHour()).toInstant(ZoneOffset.UTC);
                final Instant wdEnd   = LocalDateTime.of(wd.date(), wd.endHour()).toInstant(ZoneOffset.UTC);

                for (final FlightLeg leg : plan.legs()) {
                    if (!leg.departureTime().isBefore(wdEnd) || !leg.arrivalTime().isAfter(wdStart)) {
                        continue;
                    }
                    for (final Segment seg : leg.segments()) {
                        if (!altitudeOverlaps(seg, wd)) {
                            continue;
                        }
                        final Optional<Node> start = PersistenceContext.repositories().nodes().ofIdentity(seg.startNodeId());
                        final Optional<Node> end   = PersistenceContext.repositories().nodes().ofIdentity(seg.endNodeId());
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
                PersistenceContext.repositories().flightPlans().save(plan);
            }

            if (added == 0) {
                PilotProtocolCodec.write(sOut, ack("No weather data matched this flight plan's route/altitude/time."));
            } else {
                final String extra = testWasVoided
                        ? " (previous test result voided — re-validation required)"
                        : "";
                PilotProtocolCodec.write(sOut, ack(added + " weather record(s) added." + extra));
            }
        } catch (final Throwable t) {
            System.err.println("[PilotServer] handleAddWeather error: " + t);
            t.printStackTrace(System.err);
            PilotProtocolCodec.write(sOut, err("Error adding weather: " + t.getMessage()));
        }
    }

    private static boolean altitudeOverlaps(final Segment seg, final WeatherData wd) {
        if (seg.altitudeSlots().isEmpty()) return true;
        final double segMin = seg.altitudeSlots().stream()
                .mapToDouble(AltitudeSlot::minimumAltitude).min().orElse(0);
        final double segMax = seg.altitudeSlots().stream()
                .mapToDouble(AltitudeSlot::maximumAltitude).max().orElse(Double.MAX_VALUE);
        return segMin <= wd.altitudeSup() && segMax >= wd.altitudeInf();
    }

    private static boolean inBoundingBox(final double lat, final double lon, final WeatherData wd) {
        return lat >= Math.min(wd.latitude1(), wd.latitude2())
            && lat <= Math.max(wd.latitude1(), wd.latitude2())
            && lon >= Math.min(wd.longitude1(), wd.longitude2())
            && lon <= Math.max(wd.longitude1(), wd.longitude2());
    }

    private void handleListFleet(final DataOutputStream sOut) throws IOException {
        if (!isAuthenticated(sOut)) return;
        try {
            final String pilotEmail = authenticatedUser.identity().toString();
            final Pilot pilot = PersistenceContext.repositories().pilots()
                    .findByUserId(UserId.valueOf(pilotEmail))
                    .orElseThrow(() -> new IllegalStateException("Pilot profile not found"));

            final PilotCompanyAssignment assignment = PersistenceContext.repositories().pilotCompanyAssignments()
                    .findByPilotUserId(pilot.userId())
                    .orElseThrow(() -> new IllegalStateException("Pilot is not assigned to a company"));

            final List<Aircraft> companyFleet = PersistenceContext.repositories().aircraft()
                    .findByAirTransportCompanyId(assignment.companyId());
            final Set<aircraftmodel.domain.AircraftModel> certified = pilot.certifiedAircraftModels();

            final StringJoiner response = new StringJoiner("\n");
            companyFleet.stream()
                    .filter(a -> certified.contains(a.aircraftModel()))
                    .forEach(a -> response.add(
                            a.registrationNumber() + ";"
                            + a.aircraftModel().modelName() + ";"
                            + a.aircraftModel().maker().name() + ";"
                            + a.operationStatus()));
            PilotProtocolCodec.write(sOut, ack(response.toString()));
        } catch (final Throwable t) {
            System.err.println("[PilotServer] handleListFleet error: " + t);
            PilotProtocolCodec.write(sOut, err("Error listing fleet: " + t.getMessage()));
        }
    }

    private void handleListRoutes(final DataOutputStream sOut) throws IOException {
        if (!isAuthenticated(sOut)) return;
        try {
            final String pilotEmail = authenticatedUser.identity().toString();
            final Pilot pilot = PersistenceContext.repositories().pilots()
                    .findByUserId(UserId.valueOf(pilotEmail))
                    .orElseThrow(() -> new IllegalStateException("Pilot profile not found"));

            final PilotCompanyAssignment assignment = PersistenceContext.repositories().pilotCompanyAssignments()
                    .findByPilotUserId(pilot.userId())
                    .orElseThrow(() -> new IllegalStateException("Pilot is not assigned to a company"));

            final StringJoiner response = new StringJoiner("\n");
            PersistenceContext.repositories().flightRoutes()
                    .findByCompanyId(assignment.companyId())
                    .forEach(r -> response.add(
                            r.designation() + ";"
                            + r.originAirport().iataCode() + ";"
                            + r.destinationAirport().iataCode() + ";"
                            + r.activeFrom()));
            PilotProtocolCodec.write(sOut, ack(response.toString()));
        } catch (final Throwable t) {
            System.err.println("[PilotServer] handleListRoutes error: " + t);
            PilotProtocolCodec.write(sOut, err("Error listing routes: " + t.getMessage()));
        }
    }

    private boolean isAuthenticated(final DataOutputStream sOut) throws IOException {
        if (authenticatedUser == null) {
            PilotProtocolCodec.write(sOut, err("Not authenticated"));
            return false;
        }
        return true;
    }

    private PilotProtocolMessage ack(final String text) {
        return new PilotProtocolMessage(PilotMessageCode.ACK, text);
    }

    private PilotProtocolMessage err(final String text) {
        return new PilotProtocolMessage(PilotMessageCode.ERR, text);
    }
}
