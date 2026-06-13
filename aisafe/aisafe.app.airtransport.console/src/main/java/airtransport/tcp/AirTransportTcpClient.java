package airtransport.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class AirTransportTcpClient implements AutoCloseable {

    private final Socket sock;
    private final DataInputStream  sIn;
    private final DataOutputStream sOut;

    public AirTransportTcpClient(final String host, final int port) throws IOException {
        final InetAddress addr = InetAddress.getByName(host);
        this.sock = new Socket(addr, port);
        this.sock.setSoTimeout(30_000);
        this.sIn  = new DataInputStream(sock.getInputStream());
        this.sOut = new DataOutputStream(sock.getOutputStream());
    }

    public boolean login(final String user, final String pass) throws IOException {
        send(AirTransportMessageCode.AUTH, user + "\0" + pass);
        return readResponse().code() == AirTransportMessageCode.ACK;
    }

    public String listAircraftModels() throws IOException {
        send(AirTransportMessageCode.LIST_AIRCRAFT_MODELS, "");
        return readResponseText();
    }

    public String listFleet() throws IOException {
        send(AirTransportMessageCode.LIST_FLEET, "");
        return readResponseText();
    }

    public String registerAircraft(final String payload) throws IOException {
        send(AirTransportMessageCode.REGISTER_AIRCRAFT, payload);
        return readResponseText();
    }

    public String decommissionAircraft(final String aircraftId) throws IOException {
        send(AirTransportMessageCode.DECOMMISSION_AIRCRAFT, aircraftId);
        return readResponseText();
    }

    public String listAirports() throws IOException {
        send(AirTransportMessageCode.LIST_AIRPORTS, "");
        return readResponseText();
    }

    public String listFlightRoutes() throws IOException {
        send(AirTransportMessageCode.LIST_FLIGHT_ROUTES, "");
        return readResponseText();
    }

    public String createFlightRoute(final String payload) throws IOException {
        send(AirTransportMessageCode.CREATE_FLIGHT_ROUTE, payload);
        return readResponseText();
    }

    public String deleteFlightRoute(final String routeId, final String date) throws IOException {
        send(AirTransportMessageCode.DELETE_FLIGHT_ROUTE, routeId + ";" + date);
        return readResponseText();
    }

    public String listFlightPlans() throws IOException {
        send(AirTransportMessageCode.LIST_FLIGHT_PLANS, "");
        return readResponseText();
    }

    public String createFlightPlan(final String dslText) throws IOException {
        send(AirTransportMessageCode.CREATE_FLIGHT_PLAN, dslText);
        return readResponseText();
    }

    public String validateFlightPlan(final String planId) throws IOException {
        send(AirTransportMessageCode.VALIDATE_FLIGHT_PLAN, planId);
        return readResponseText();
    }

    public String addWeatherData(final String planId) throws IOException {
        send(AirTransportMessageCode.ADD_WEATHER_DATA, planId);
        return readResponseText();
    }

    public String listCompanyPilots() throws IOException {
        send(AirTransportMessageCode.LIST_COMPANY_PILOTS, "");
        return readResponseText();
    }

    public String listAvailablePilots() throws IOException {
        send(AirTransportMessageCode.LIST_AVAILABLE_PILOTS, "");
        return readResponseText();
    }

    public String addPilot(final String userId) throws IOException {
        send(AirTransportMessageCode.ADD_PILOT, userId);
        return readResponseText();
    }

    public String removePilot(final String userId) throws IOException {
        send(AirTransportMessageCode.REMOVE_PILOT, userId);
        return readResponseText();
    }

    public String activatePilot(final String userId) throws IOException {
        send(AirTransportMessageCode.ACTIVATE_PILOT, userId);
        return readResponseText();
    }

    public String deactivatePilot(final String userId) throws IOException {
        send(AirTransportMessageCode.DEACTIVATE_PILOT, userId);
        return readResponseText();
    }

    @Override
    public void close() throws IOException {
        try { send(AirTransportMessageCode.DISCONN, ""); } catch (final IOException ignored) {}
        sock.close();
    }

    private void send(final AirTransportMessageCode code, final String data) throws IOException {
        AirTransportProtocolCodec.write(sOut, new AirTransportProtocolMessage(code, data));
    }

    private AirTransportProtocolMessage readResponse() throws IOException {
        return AirTransportProtocolCodec.read(sIn);
    }

    private String readResponseText() throws IOException {
        return readResponse().dataAsString();
    }
}
