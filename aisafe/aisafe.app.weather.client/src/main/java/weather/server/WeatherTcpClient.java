package weather.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class WeatherTcpClient implements AutoCloseable {

    private final Socket sock;
    private final DataInputStream sIn;
    private final DataOutputStream sOut;

    public WeatherTcpClient(String host, int port) throws IOException {
        InetAddress addr = InetAddress.getByName(host);       // dual-stack IPv4/IPv6
        this.sock = new Socket(addr, port);                   // slide 5
        this.sock.setSoTimeout(10_000);                       // protege contra bloqueio
        this.sIn  = new DataInputStream(sock.getInputStream());
        this.sOut = new DataOutputStream(sock.getOutputStream());
    }

    public boolean login(String user, String pass) throws IOException {
        ProtocolCodec.write(sOut, new ProtocolMessage(MessageCode.AUTH, user + "\0" + pass));
        return ProtocolCodec.read(sIn).code() == MessageCode.ACK;
    }

    public String uploadWeather(String jsonPayload) throws IOException {
        ProtocolCodec.write(sOut, new ProtocolMessage(MessageCode.UPLOAD_WEATHER, jsonPayload));
        return ProtocolCodec.read(sIn).dataAsString();
    }

    public String listWeather(String area, String date) throws IOException {
        String payload = (area == null ? "" : area) + ";" + (date == null ? "" : date);
        ProtocolCodec.write(sOut, new ProtocolMessage(MessageCode.LIST_WEATHER, payload));
        return ProtocolCodec.read(sIn).dataAsString();
    }

    @Override
    public void close() throws IOException {
        try { ProtocolCodec.write(sOut, new ProtocolMessage(MessageCode.DISCONN, "")); }
        catch (IOException ignored) {}
        sock.close();
    }
}