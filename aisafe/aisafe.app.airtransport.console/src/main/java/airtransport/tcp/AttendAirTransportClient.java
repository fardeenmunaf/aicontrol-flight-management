package airtransport.tcp;

import airtransport.service.LocalAirTransportService;
import auth.domain.AISafeRoles;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import infrastructure.persistence.PersistenceContext;
import remotelogging.domain.RemoteAccessEventType;
import remotelogging.domain.RemoteService;
import remotelogging.udp.UdpLogSender;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class AttendAirTransportClient implements Runnable {

    private final Socket cliSock;
    private SystemUser authenticatedUser;
    private LocalAirTransportService localService;

    public AttendAirTransportClient(final Socket s) {
        this.cliSock = s;
    }

    @Override
    public void run() {
        final String clientIp   = cliSock.getInetAddress().getHostAddress();
        final int    clientPort = cliSock.getPort();
        try (cliSock;
             DataInputStream  sIn  = new DataInputStream(cliSock.getInputStream());
             DataOutputStream sOut = new DataOutputStream(cliSock.getOutputStream())) {

            System.out.println("[ATCServer] Client connected from " + clientIp + ":" + clientPort);

            boolean running = true;
            while (running) {
                final AirTransportProtocolMessage msg = AirTransportProtocolCodec.read(sIn);
                switch (msg.code()) {
                    case COMMTEST              -> AirTransportProtocolCodec.write(sOut, ack("OK"));
                    case AUTH                  -> handleAuth(msg, sOut, clientIp, clientPort);
                    case LIST_AIRCRAFT_MODELS  -> handle(sOut, () -> localService.listAircraftModels());
                    case LIST_FLEET            -> handle(sOut, () -> localService.listFleet());
                    case REGISTER_AIRCRAFT     -> handleRegisterAircraft(msg, sOut);
                    case DECOMMISSION_AIRCRAFT -> handle(sOut, () -> localService.decommissionAircraft(msg.dataAsString().trim()));
                    case LIST_AIRPORTS         -> handle(sOut, () -> localService.listAirports());
                    case LIST_FLIGHT_ROUTES    -> handle(sOut, () -> localService.listFlightRoutes());
                    case CREATE_FLIGHT_ROUTE   -> handleCreateRoute(msg, sOut);
                    case DELETE_FLIGHT_ROUTE   -> handleDeleteRoute(msg, sOut);
                    case LIST_FLIGHT_PLANS     -> handle(sOut, () -> localService.listFlightPlans());
                    case CREATE_FLIGHT_PLAN    -> handle(sOut, () -> localService.createFlightPlan(msg.dataAsString()));
                    case VALIDATE_FLIGHT_PLAN  -> handle(sOut, () -> localService.validateFlightPlan(msg.dataAsString().trim()));
                    case ADD_WEATHER_DATA       -> handle(sOut, () -> localService.addWeatherData(msg.dataAsString().trim()));
                    case LIST_COMPANY_PILOTS   -> handle(sOut, () -> localService.listCompanyPilots());
                    case LIST_AVAILABLE_PILOTS -> handle(sOut, () -> localService.listAvailablePilots());
                    case ADD_PILOT             -> handle(sOut, () -> localService.addPilotToCompany(msg.dataAsString().trim()));
                    case REMOVE_PILOT          -> handle(sOut, () -> localService.removePilotFromCompany(msg.dataAsString().trim()));
                    case ACTIVATE_PILOT        -> handle(sOut, () -> localService.activatePilot(msg.dataAsString().trim()));
                    case DEACTIVATE_PILOT      -> handle(sOut, () -> localService.deactivatePilot(msg.dataAsString().trim()));
                    case DISCONN               -> {
                        AirTransportProtocolCodec.write(sOut, ack("BYE"));
                        running = false;
                        if (authenticatedUser != null) {
                            UdpLogSender.instance().log(
                                    authenticatedUser.username().toString(),
                                    clientIp, clientPort,
                                    RemoteService.US078, RemoteAccessEventType.LOGOUT);
                        }
                    }
                    default -> AirTransportProtocolCodec.write(sOut, err("Unsupported operation"));
                }
            }
        } catch (final IOException e) {
            System.err.println("[ATCServer] Connection ended: " + e.getMessage());
            if (authenticatedUser != null) {
                UdpLogSender.instance().log(
                        authenticatedUser.username().toString(),
                        clientIp, clientPort,
                        RemoteService.US078, RemoteAccessEventType.DISCONNECT);
            }
        }
    }

    private void handleAuth(final AirTransportProtocolMessage msg, final DataOutputStream sOut,
                            final String ip, final int port) throws IOException {
        final String[] parts = msg.dataAsString().split("\0");
        if (parts.length != 2) {
            AirTransportProtocolCodec.write(sOut, err("Invalid auth format"));
            return;
        }
        final String user = parts[0];
        final String pass = parts[1];

        final Optional<UserSession> session = AuthzRegistry.authenticationService().authenticate(user, pass);
        if (session.isEmpty()) {
            UdpLogSender.instance().log(user, ip, port, RemoteService.US078, RemoteAccessEventType.FAILED_LOGIN);
            AirTransportProtocolCodec.write(sOut, err("Invalid credentials"));
            return;
        }

        this.authenticatedUser = session.get().authenticatedUser();
        if (!authenticatedUser.roleTypes().contains(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            UdpLogSender.instance().log(user, ip, port, RemoteService.US078, RemoteAccessEventType.FAILED_LOGIN);
            this.authenticatedUser = null;
            AirTransportProtocolCodec.write(sOut, err("Not authorized: ATCC role required"));
            return;
        }

        try {
            final CustomerCollaborator collab = PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(UserId.valueOf(user))
                    .orElseThrow(() -> new IllegalStateException("No customer collaborator profile"));
            if (collab.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                throw new IllegalStateException("Not linked to an air transport company");
            }
            final AirTransportCompanyId companyId = collab.airTransportCompanyId();
            final String icaoCode = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(companyId)
                    .map(c -> c.icaoCode())
                    .orElse("");
            this.localService = new LocalAirTransportService(companyId, icaoCode);
            UdpLogSender.instance().log(user, ip, port, RemoteService.US078, RemoteAccessEventType.SUCCESSFUL_LOGIN);
            AirTransportProtocolCodec.write(sOut, ack("Authenticated as ATCC"));
        } catch (final Exception e) {
            this.authenticatedUser = null;
            UdpLogSender.instance().log(user, ip, port, RemoteService.US078, RemoteAccessEventType.FAILED_LOGIN);
            AirTransportProtocolCodec.write(sOut, err("Auth error: " + e.getMessage()));
        }
    }

    private void handle(final DataOutputStream sOut, final CheckedSupplier op) throws IOException {
        if (localService == null) {
            AirTransportProtocolCodec.write(sOut, err("Not authenticated"));
            return;
        }
        try {
            AirTransportProtocolCodec.write(sOut, ack(op.get()));
        } catch (final Throwable t) {
            System.err.println("[ATCServer] Operation error: " + t);
            AirTransportProtocolCodec.write(sOut, err("Error: " + t.getMessage()));
        }
    }

    private void handleRegisterAircraft(final AirTransportProtocolMessage msg, final DataOutputStream sOut) throws IOException {
        if (localService == null) { AirTransportProtocolCodec.write(sOut, err("Not authenticated")); return; }
        try {
            final String[] p = msg.dataAsString().split(";", -1);
            if (p.length < 9) { AirTransportProtocolCodec.write(sOut, err("Invalid register aircraft format")); return; }
            AirTransportProtocolCodec.write(sOut, ack(localService.registerAircraft(
                    p[0], p[1], p[2], Integer.parseInt(p[3]), p[4], p[5],
                    Integer.parseInt(p[6]), Integer.parseInt(p[7]), Integer.parseInt(p[8]))));
        } catch (final Throwable t) {
            AirTransportProtocolCodec.write(sOut, err("Error: " + t.getMessage()));
        }
    }

    private void handleCreateRoute(final AirTransportProtocolMessage msg, final DataOutputStream sOut) throws IOException {
        if (localService == null) { AirTransportProtocolCodec.write(sOut, err("Not authenticated")); return; }
        try {
            final String[] p = msg.dataAsString().split(";", -1);
            if (p.length < 4) { AirTransportProtocolCodec.write(sOut, err("Invalid create route format")); return; }
            AirTransportProtocolCodec.write(sOut, ack(localService.createFlightRoute(p[0], p[1], p[2], p[3])));
        } catch (final Throwable t) {
            AirTransportProtocolCodec.write(sOut, err("Error: " + t.getMessage()));
        }
    }

    private void handleDeleteRoute(final AirTransportProtocolMessage msg, final DataOutputStream sOut) throws IOException {
        if (localService == null) { AirTransportProtocolCodec.write(sOut, err("Not authenticated")); return; }
        try {
            final String[] p = msg.dataAsString().split(";", 2);
            if (p.length < 2) { AirTransportProtocolCodec.write(sOut, err("Invalid delete route format")); return; }
            AirTransportProtocolCodec.write(sOut, ack(localService.deleteFlightRoute(p[0], p[1])));
        } catch (final Throwable t) {
            AirTransportProtocolCodec.write(sOut, err("Error: " + t.getMessage()));
        }
    }

    private AirTransportProtocolMessage ack(final String text) {
        return new AirTransportProtocolMessage(AirTransportMessageCode.ACK, text);
    }

    private AirTransportProtocolMessage err(final String text) {
        return new AirTransportProtocolMessage(AirTransportMessageCode.ERR, text);
    }

    @FunctionalInterface
    private interface CheckedSupplier {
        String get() throws Exception;
    }
}
