package remotelogging.udp;

import remotelogging.domain.RemoteAccessEvent;
import remotelogging.domain.RemoteAccessEventType;
import remotelogging.domain.RemoteService;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Sends remote access log events to the Remote Accesses Logging Server via UDP.
 *
 * Transmission is non-blocking: each datagram is sent in a daemon thread so that
 * a slow or unreachable logging server never delays or interrupts the caller (AC5).
 *
 * Address and port are configurable via system properties or environment variables
 * without requiring code changes (AC1):
 *   - System property:  aisafe.logging.host / aisafe.logging.port
 *   - Environment var:  AISAFE_LOGGING_HOST / AISAFE_LOGGING_PORT
 *   - Default:          localhost / 9999
 */
public class UdpLogSender {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 9999;

    private final String host;
    private final int    port;

    private static final UdpLogSender INSTANCE = new UdpLogSender();

    private UdpLogSender() {
        this.host = resolveHost();
        this.port = resolvePort();
    }

    public static UdpLogSender instance() {
        return INSTANCE;
    }

    public void send(final RemoteAccessEvent event) {
        final String payload = event.serialize();
        final Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                final byte[] data = payload.getBytes(StandardCharsets.UTF_8);
                final InetAddress address = InetAddress.getByName(host);
                final DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet);
            } catch (final Exception e) {
                // AC5: failure to reach the logging server must not affect service
                System.err.println("[UdpLogSender] Could not send log datagram: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void log(final String username,
                    final String clientIp,
                    final int clientPort,
                    final RemoteService service,
                    final RemoteAccessEventType eventType) {
        send(new RemoteAccessEvent(username, clientIp, clientPort, service, eventType));
    }

    private static String resolveHost() {
        final String prop = System.getProperty("aisafe.logging.host");
        if (prop != null && !prop.isBlank()) return prop;
        final String env = System.getenv("AISAFE_LOGGING_HOST");
        if (env != null && !env.isBlank()) return env;
        return DEFAULT_HOST;
    }

    private static int resolvePort() {
        try {
            final String prop = System.getProperty("aisafe.logging.port");
            if (prop != null && !prop.isBlank()) return Integer.parseInt(prop);
            final String env = System.getenv("AISAFE_LOGGING_PORT");
            if (env != null && !env.isBlank()) return Integer.parseInt(env);
        } catch (final NumberFormatException ignored) {}
        return DEFAULT_PORT;
    }
}
