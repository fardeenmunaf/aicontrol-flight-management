package logging;

import remotelogging.domain.RemoteAccessEvent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote Accesses Logging Server — US090.
 *
 * Standalone application that receives UDP datagrams from remote access
 * services (US044, US078, US086) and stores them as log entries.
 *
 * Configuration (system property or environment variable):
 *   aisafe.logging.port / AISAFE_LOGGING_PORT  (default: 9999)
 *   aisafe.logging.dir  / AISAFE_LOGGING_DIR   (default: logs/)
 *
 * Log file: logs/remote-access-YYYY-MM-DD.log (one file per day).
 *
 * Duplicate detection: datagrams with identical content received within the
 * same second are discarded (AC4 — handles duplicates gracefully).
 */
public class RemoteAccessesLoggingServer {

    private static final int    DEFAULT_PORT       = 9999;
    private static final int    MAX_DATAGRAM_BYTES = 1024;
    private static final String DEFAULT_LOG_DIR    = "logs";

    private final int    port;
    private final Path   logDir;
    private final ConcurrentHashMap<String, Long> recentEntries = new ConcurrentHashMap<>();

    public RemoteAccessesLoggingServer(final int port, final Path logDir) {
        this.port   = port;
        this.logDir = logDir;
    }

    public void start() {
        try {
            Files.createDirectories(logDir);
        } catch (final Exception e) {
            System.err.println("Could not create log directory: " + e.getMessage());
            return;
        }

        System.out.printf("Remote Accesses Logging Server started on UDP port %d%n", port);
        System.out.printf("Log directory: %s%n", logDir.toAbsolutePath());
        System.out.println("Waiting for log datagrams... (Ctrl+C to stop)");

        try (DatagramSocket socket = new DatagramSocket(port)) {
            final byte[] buffer = new byte[MAX_DATAGRAM_BYTES];
            while (true) {
                final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                final String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                handleDatagram(payload);
            }
        } catch (final Exception e) {
            System.err.println("Logging server error: " + e.getMessage());
        }
    }

    private void handleDatagram(final String payload) {
        try {
            final RemoteAccessEvent event = RemoteAccessEvent.deserialize(payload);

            if (isDuplicate(payload)) {
                System.out.println("[DUPLICATE] Discarded: " + payload);
                return;
            }

            final String logLine = buildLogLine(event);
            writeToFile(logLine);
            System.out.println("[LOG] " + event);

        } catch (final Exception e) {
            System.err.println("[WARN] Malformed datagram discarded: " + payload + " — " + e.getMessage());
        }
    }

    private boolean isDuplicate(final String payload) {
        final long nowSecond = System.currentTimeMillis() / 1000;
        final Long prev = recentEntries.put(payload, nowSecond);
        if (prev != null && (nowSecond - prev) < 2) {
            return true;
        }
        if (recentEntries.size() > 10_000) {
            recentEntries.clear();
        }
        return false;
    }

    private String buildLogLine(final RemoteAccessEvent event) {
        return String.format("%s | %-20s | %-15s | %-5d | %-6s | %s%n",
                event.timestamp(),
                event.username(),
                event.clientIp(),
                event.clientPort(),
                event.service().name(),
                event.eventType().name());
    }

    private void writeToFile(final String line) {
        final String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        final Path logFile = logDir.resolve("remote-access-" + today + ".log");
        try {
            Files.writeString(logFile, line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (final Exception e) {
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        final int port = resolvePort();
        final Path dir = Paths.get(resolveDir());
        new RemoteAccessesLoggingServer(port, dir).start();
    }

    private static int resolvePort() {
        try {
            String v = System.getProperty("aisafe.logging.port");
            if (v != null && !v.isBlank()) return Integer.parseInt(v);
            v = System.getenv("AISAFE_LOGGING_PORT");
            if (v != null && !v.isBlank()) return Integer.parseInt(v);
        } catch (final NumberFormatException ignored) {}
        return DEFAULT_PORT;
    }

    private static String resolveDir() {
        String v = System.getProperty("aisafe.logging.dir");
        if (v != null && !v.isBlank()) return v;
        v = System.getenv("AISAFE_LOGGING_DIR");
        if (v != null && !v.isBlank()) return v;
        return DEFAULT_LOG_DIR;
    }
}
