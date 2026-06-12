package pilot.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCP client stub used by the Pilot Client presentation layer.
 * All operations go through the TCP connection — no direct database access.
 */
public class PilotTcpClient implements AutoCloseable {

    private final Socket sock;
    private final DataInputStream  sIn;
    private final DataOutputStream sOut;

    public PilotTcpClient(final String host, final int port) throws IOException {
        final InetAddress addr = InetAddress.getByName(host);
        this.sock = new Socket(addr, port);
        this.sock.setSoTimeout(30_000);
        this.sIn  = new DataInputStream(sock.getInputStream());
        this.sOut = new DataOutputStream(sock.getOutputStream());
    }

    public boolean login(final String user, final String pass) throws IOException {
        send(PilotMessageCode.AUTH, user + "\0" + pass);
        return readResponse().code() == PilotMessageCode.ACK;
    }

    public String listMyPlans() throws IOException {
        send(PilotMessageCode.LIST_MY_PLANS, "");
        return readResponseText();
    }

    public String createPlan(final String dslText) throws IOException {
        send(PilotMessageCode.CREATE_PLAN, dslText);
        return readResponseText();
    }

    public String validatePlan(final String designator) throws IOException {
        send(PilotMessageCode.VALIDATE_PLAN, designator);
        return readResponseText();
    }

    public String addWeather(final String designator) throws IOException {
        send(PilotMessageCode.ADD_WEATHER, designator);
        return readResponseText();
    }

    public String listFleet() throws IOException {
        send(PilotMessageCode.LIST_FLEET, "");
        return readResponseText();
    }

    public String listRoutes() throws IOException {
        send(PilotMessageCode.LIST_ROUTES, "");
        return readResponseText();
    }

    @Override
    public void close() throws IOException {
        try { send(PilotMessageCode.DISCONN, ""); } catch (final IOException ignored) {}
        sock.close();
    }

    private void send(final PilotMessageCode code, final String data) throws IOException {
        PilotProtocolCodec.write(sOut, new PilotProtocolMessage(code, data));
    }

    private PilotProtocolMessage readResponse() throws IOException {
        return PilotProtocolCodec.read(sIn);
    }

    private String readResponseText() throws IOException {
        return readResponse().dataAsString();
    }
}
