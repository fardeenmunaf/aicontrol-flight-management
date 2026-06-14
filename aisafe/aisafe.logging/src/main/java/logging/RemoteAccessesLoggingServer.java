package logging;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import remotelogging.domain.RemoteAccessEvent;
import remotelogging.domain.RemoteAccessEventType;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Remote Accesses Logging Server — US090 / US091.
 *
 * Receives UDP datagrams from remote access services (US044, US078, US086)
 * and stores them as log entries.  Also exposes an HTTP server (US091) with
 * AJAX status pages for administrators.
 *
 * UDP configuration:
 *   aisafe.logging.port / AISAFE_LOGGING_PORT  (default: 9999)
 *   aisafe.logging.dir  / AISAFE_LOGGING_DIR   (default: logs/)
 *
 * HTTP configuration:
 *   aisafe.logging.http.port / AISAFE_LOGGING_HTTP_PORT  (default: 8080)
 *
 * TLS configuration (optional — turns the status server into HTTPS, as required
 * for an administrator-facing service running on a dedicated cloud node, US90):
 *   aisafe.logging.tls.keystore          / AISAFE_LOGGING_TLS_KEYSTORE
 *   aisafe.logging.tls.keystore.password / AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD
 * The keystore is a PKCS12 file holding a self-signed certificate and its
 * private key (see generate-logging-keystore.sh). If not set, the status
 * server falls back to plain HTTP.
 *
 * Log file: logs/remote-access-YYYY-MM-DD.log (one file per day).
 */
public class RemoteAccessesLoggingServer {

    private static final int    DEFAULT_UDP_PORT   = 9999;
    private static final int    DEFAULT_HTTP_PORT  = 8080;
    private static final int    MAX_DATAGRAM_BYTES = 1024;
    private static final int    MAX_RECENT_EVENTS  = 100;
    private static final String DEFAULT_LOG_DIR    = "logs";

    private final int  udpPort;
    private final int  httpPort;
    private final Path logDir;

    private final ConcurrentHashMap<String, Long>   recentEntries = new ConcurrentHashMap<>();
    private final Deque<RemoteAccessEvent>           recentEvents  = new ArrayDeque<>();
    private final Map<String, RemoteAccessEvent>     activeUsers   = new LinkedHashMap<>();

    public RemoteAccessesLoggingServer(final int udpPort, final int httpPort, final Path logDir) {
        this.udpPort  = udpPort;
        this.httpPort = httpPort;
        this.logDir   = logDir;
    }

    public void start() {
        try {
            Files.createDirectories(logDir);
        } catch (final Exception e) {
            System.err.println("Could not create log directory: " + e.getMessage());
            return;
        }

        startHttpServer();

        System.out.printf("Remote Accesses Logging Server (UDP) started on port %d%n", udpPort);
        System.out.printf("Log directory: %s%n", logDir.toAbsolutePath());
        System.out.println("Waiting for log datagrams... (Ctrl+C to stop)");

        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
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

    // ── UDP handling ─────────────────────────────────────────────────────────

    private void handleDatagram(final String payload) {
        try {
            final RemoteAccessEvent event = RemoteAccessEvent.deserialize(payload);
            if (isDuplicate(payload)) {
                System.out.println("[DUPLICATE] Discarded: " + payload);
                return;
            }
            recordEvent(event);
            writeToFile(buildLogLine(event));
            System.out.println("[LOG] " + event);
        } catch (final Exception e) {
            System.err.println("[WARN] Malformed datagram discarded: " + payload + " — " + e.getMessage());
        }
    }

    private synchronized void recordEvent(final RemoteAccessEvent event) {
        if (recentEvents.size() >= MAX_RECENT_EVENTS) {
            recentEvents.pollFirst();
        }
        recentEvents.addLast(event);

        final String user = event.username();
        if (event.eventType() == RemoteAccessEventType.SUCCESSFUL_LOGIN) {
            activeUsers.put(user, event);
        } else if (event.eventType() == RemoteAccessEventType.LOGOUT
                || event.eventType() == RemoteAccessEventType.DISCONNECT) {
            activeUsers.remove(user);
        }
    }

    private boolean isDuplicate(final String payload) {
        final long nowSecond = System.currentTimeMillis() / 1000;
        final Long prev = recentEntries.put(payload, nowSecond);
        if (prev != null && (nowSecond - prev) < 2) return true;
        if (recentEntries.size() > 10_000) recentEntries.clear();
        return false;
    }

    private String buildLogLine(final RemoteAccessEvent event) {
        return String.format("%s | %-20s | %-15s | %-5d | %-6s | %s%n",
                event.timestamp(), event.username(), event.clientIp(),
                event.clientPort(), event.service().name(), event.eventType().name());
    }

    private void writeToFile(final String line) {
        final Path logFile = logDir.resolve(
                "remote-access-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log");
        try {
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final Exception e) {
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }

    // ── HTTP Server (US091) ───────────────────────────────────────────────────

    private void startHttpServer() {
        try {
            final HttpServer http = createHttpServer();
            http.createContext("/", this::handleRoot);
            http.createContext("/events", this::handleEventsPage);
            http.createContext("/active-users", this::handleActiveUsersPage);
            http.createContext("/api/events", this::handleApiEvents);
            http.createContext("/api/active-users", this::handleApiActiveUsers);
            http.setExecutor(Executors.newFixedThreadPool(4));
            http.start();
            final String scheme = (http instanceof HttpsServer) ? "https" : "http";
            System.out.printf("HTTP status server started on %s://localhost:%d%n", scheme, httpPort);
            System.out.printf("  → Last events:    %s://localhost:%d/events%n", scheme, httpPort);
            System.out.printf("  → Active users:   %s://localhost:%d/active-users%n", scheme, httpPort);
        } catch (final Exception e) {
            System.err.println("Could not start HTTP server on port " + httpPort + ": " + e.getMessage());
        }
    }

    /**
     * Creates the status HTTP server. If a TLS keystore is configured, an
     * {@link HttpsServer} secured with a self-signed certificate is created
     * (TP11 — SSL/TLS network programming), otherwise a plain HTTP server.
     */
    private HttpServer createHttpServer() throws Exception {
        final String keystorePath = resolveKeystorePath();
        if (keystorePath == null || keystorePath.isBlank()) {
            return HttpServer.create(new InetSocketAddress(httpPort), 0);
        }

        final SSLContext sslContext = buildSslContext(Paths.get(keystorePath), resolveKeystorePassword());
        final HttpsServer https = HttpsServer.create(new InetSocketAddress(httpPort), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        return https;
    }

    /** Loads a self-signed certificate + private key (PKCS12) into an SSLContext. */
    private static SSLContext buildSslContext(final Path keystorePath, final char[] password) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystorePath)) {
            keyStore.load(in, password);
        }

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

    private void handleRoot(final HttpExchange ex) throws IOException {
        final String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<title>AISafe Logging Server</title>"
                + "<style>body{font-family:sans-serif;margin:40px;} a{display:block;margin:10px;font-size:1.2em;}</style>"
                + "</head><body>"
                + "<h1>AISafe Remote Accesses Logging Server (US091)</h1>"
                + "<a href='/events'>📋 Last Recorded Events</a>"
                + "<a href='/active-users'>👥 Currently Active Users</a>"
                + "</body></html>";
        sendHtml(ex, html);
    }

    private void handleEventsPage(final HttpExchange ex) throws IOException {
        final String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<title>Last Events — AISafe</title>"
                + "<style>"
                + "body{font-family:sans-serif;margin:20px;background:#f5f5f5;}"
                + "h1{color:#333;} table{border-collapse:collapse;width:100%;background:#fff;}"
                + "th{background:#2c3e50;color:#fff;padding:8px 12px;text-align:left;}"
                + "td{padding:7px 12px;border-bottom:1px solid #ddd;}"
                + "tr:hover td{background:#eef;}"
                + ".badge{padding:2px 8px;border-radius:4px;font-size:.85em;color:#fff;}"
                + ".LOGIN{background:#27ae60;} .FAILED{background:#e74c3c;}"
                + ".LOGOUT{background:#2980b9;} .DISCONNECT{background:#7f8c8d;}"
                + "#status{color:#888;font-size:.9em;margin-top:8px;}"
                + "nav a{margin-right:15px;}"
                + "</style></head><body>"
                + "<nav><a href='/'>Home</a><a href='/active-users'>Active Users</a></nav>"
                + "<h1>Last Recorded Events</h1>"
                + "<div id='status'>Loading...</div>"
                + "<table id='evtTable'>"
                + "<thead><tr><th>Timestamp</th><th>User</th><th>IP</th><th>Port</th><th>Service</th><th>Event</th></tr></thead>"
                + "<tbody id='evtBody'></tbody></table>"
                + "<script>"
                + "function badgeClass(t){if(t.includes('SUCCESSFUL'))return 'LOGIN';if(t.includes('FAILED'))return 'FAILED';if(t.includes('LOGOUT'))return 'LOGOUT';return 'DISCONNECT';}"
                + "function refresh(){"
                + "  fetch('/api/events').then(r=>r.json()).then(data=>{"
                + "    const tb=document.getElementById('evtBody');"
                + "    tb.innerHTML='';"
                + "    data.forEach(e=>{"
                + "      const cls=badgeClass(e.eventType);"
                + "      tb.innerHTML+=`<tr><td>${e.timestamp}</td><td>${e.username}</td><td>${e.clientIp}</td><td>${e.clientPort}</td><td>${e.service}</td><td><span class='badge ${cls}'>${e.eventType}</span></td></tr>`;"
                + "    });"
                + "    document.getElementById('status').textContent='Last updated: '+new Date().toLocaleTimeString()+' ('+data.length+' events)';"
                + "  }).catch(err=>document.getElementById('status').textContent='Error: '+err);"
                + "}"
                + "refresh(); setInterval(refresh, 3000);"
                + "</script></body></html>";
        sendHtml(ex, html);
    }

    private void handleActiveUsersPage(final HttpExchange ex) throws IOException {
        final String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<title>Active Users — AISafe</title>"
                + "<style>"
                + "body{font-family:sans-serif;margin:20px;background:#f5f5f5;}"
                + "h1{color:#333;} table{border-collapse:collapse;width:100%;background:#fff;}"
                + "th{background:#2c3e50;color:#fff;padding:8px 12px;text-align:left;}"
                + "td{padding:7px 12px;border-bottom:1px solid #ddd;}"
                + "tr:hover td{background:#eef;}"
                + ".active{color:#27ae60;font-weight:bold;}"
                + "#status{color:#888;font-size:.9em;margin-top:8px;}"
                + "nav a{margin-right:15px;}"
                + "</style></head><body>"
                + "<nav><a href='/'>Home</a><a href='/events'>Last Events</a></nav>"
                + "<h1>Currently Active Remote Users</h1>"
                + "<div id='status'>Loading...</div>"
                + "<table id='auTable'>"
                + "<thead><tr><th>Username</th><th>IP Address</th><th>Port</th><th>Service</th><th>Login Time</th></tr></thead>"
                + "<tbody id='auBody'></tbody></table>"
                + "<script>"
                + "function refresh(){"
                + "  fetch('/api/active-users').then(r=>r.json()).then(data=>{"
                + "    const tb=document.getElementById('auBody');"
                + "    if(data.length===0){"
                + "      tb.innerHTML='<tr><td colspan=5 style=\"text-align:center;color:#999;\">No active users</td></tr>';"
                + "    }else{"
                + "      tb.innerHTML='';"
                + "      data.forEach(e=>{"
                + "        tb.innerHTML+=`<tr><td class='active'>${e.username}</td><td>${e.clientIp}</td><td>${e.clientPort}</td><td>${e.service}</td><td>${e.timestamp}</td></tr>`;"
                + "      });"
                + "    }"
                + "    document.getElementById('status').textContent='Last updated: '+new Date().toLocaleTimeString()+' ('+data.length+' active)';"
                + "  }).catch(err=>document.getElementById('status').textContent='Error: '+err);"
                + "}"
                + "refresh(); setInterval(refresh, 3000);"
                + "</script></body></html>";
        sendHtml(ex, html);
    }

    private synchronized void handleApiEvents(final HttpExchange ex) throws IOException {
        final StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (final RemoteAccessEvent e : recentEvents) {
            if (!first) json.append(",");
            json.append(toJson(e));
            first = false;
        }
        json.append("]");
        sendJson(ex, json.toString());
    }

    private synchronized void handleApiActiveUsers(final HttpExchange ex) throws IOException {
        final StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (final RemoteAccessEvent e : activeUsers.values()) {
            if (!first) json.append(",");
            json.append(toJson(e));
            first = false;
        }
        json.append("]");
        sendJson(ex, json.toString());
    }

    private static String toJson(final RemoteAccessEvent e) {
        return String.format(
                "{\"timestamp\":\"%s\",\"username\":\"%s\",\"clientIp\":\"%s\","
                + "\"clientPort\":%d,\"service\":\"%s\",\"eventType\":\"%s\"}",
                e.timestamp(), escapeJson(e.username()), escapeJson(e.clientIp()),
                e.clientPort(), e.service().name(), e.eventType().name());
    }

    private static String escapeJson(final String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sendHtml(final HttpExchange ex, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void sendJson(final HttpExchange ex, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    public static void main(final String[] args) {
        final int  udpPort  = resolveUdpPort();
        final int  httpPort = resolveHttpPort();
        final Path dir      = Paths.get(resolveDir());
        new RemoteAccessesLoggingServer(udpPort, httpPort, dir).start();
    }

    private static int resolveUdpPort() {
        try {
            String v = System.getProperty("aisafe.logging.port");
            if (v != null && !v.isBlank()) return Integer.parseInt(v);
            v = System.getenv("AISAFE_LOGGING_PORT");
            if (v != null && !v.isBlank()) return Integer.parseInt(v);
        } catch (final NumberFormatException ignored) {}
        return DEFAULT_UDP_PORT;
    }

    private static int resolveHttpPort() {
        try {
            String v = System.getProperty("aisafe.logging.http.port");
            if (v != null && !v.isBlank()) return Integer.parseInt(v);
            v = System.getenv("AISAFE_LOGGING_HTTP_PORT");
            if (v != null && !v.isBlank()) return Integer.parseInt(v);
        } catch (final NumberFormatException ignored) {}
        return DEFAULT_HTTP_PORT;
    }

    private static String resolveDir() {
        String v = System.getProperty("aisafe.logging.dir");
        if (v != null && !v.isBlank()) return v;
        v = System.getenv("AISAFE_LOGGING_DIR");
        if (v != null && !v.isBlank()) return v;
        return DEFAULT_LOG_DIR;
    }

    private static String resolveKeystorePath() {
        String v = System.getProperty("aisafe.logging.tls.keystore");
        if (v != null && !v.isBlank()) return v;
        v = System.getenv("AISAFE_LOGGING_TLS_KEYSTORE");
        if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static char[] resolveKeystorePassword() {
        String v = System.getProperty("aisafe.logging.tls.keystore.password");
        if (v != null && !v.isBlank()) return v.toCharArray();
        v = System.getenv("AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD");
        if (v != null && !v.isBlank()) return v.toCharArray();
        return "Teste1234!".toCharArray();
    }
}
