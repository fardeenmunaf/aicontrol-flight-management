package logging;

import org.junit.jupiter.api.*;
import remotelogging.domain.RemoteAccessEventType;
import remotelogging.domain.RemoteService;

import java.io.File;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RemoteAccessesLoggingServerTest {

    private static final int UDP  = 19997;
    private static final int HTTP = 18097;
    private static Path logDir;

    @BeforeAll
    static void startServer() throws Exception {
        logDir = Files.createTempDirectory("aisafe-log-test-");
        Thread t = new Thread(new RemoteAccessesLoggingServer(UDP, HTTP, logDir)::start);
        t.setDaemon(true);
        t.start();
        Thread.sleep(600);
    }

    @AfterAll
    static void cleanup() throws Exception {
        Files.walk(logDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String get(final String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:" + HTTP + path).openConnection();
        return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static int status(final String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:" + HTTP + path).openConnection();
        return c.getResponseCode();
    }

    private static void udp(final String payload) throws Exception {
        try (DatagramSocket s = new DatagramSocket()) {
            byte[] b = payload.getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(b, b.length, InetAddress.getLoopbackAddress(), UDP));
        }
        Thread.sleep(150);
    }

    private static String ts() {
        return java.time.Instant.now().toString();
    }

    private static int count(final String text, final String pattern) {
        int c = 0, i = 0;
        while ((i = text.indexOf(pattern, i)) != -1) { c++; i += pattern.length(); }
        return c;
    }

    // ── AC1 — HTTP server on configurable port ────────────────────────────────

    @Test @Order(1)
    void rootPageReturns200() throws Exception {
        assertEquals(200, status("/"));
    }

    @Test @Order(2)
    void rootPageContainsBothPageLinks() throws Exception {
        String body = get("/");
        assertTrue(body.contains("/events"));
        assertTrue(body.contains("/active-users"));
    }

    // ── AC2 — Last recorded events page ──────────────────────────────────────

    @Test @Order(3)
    void eventsPageReturns200() throws Exception {
        assertEquals(200, status("/events"));
    }

    @Test @Order(4)
    void eventsPageHasAjaxRefresh() throws Exception {
        String body = get("/events");
        assertTrue(body.contains("fetch"));
        assertTrue(body.contains("setInterval"));
    }

    @Test @Order(5)
    void eventsPageContainsAllRequiredColumnHeaders() throws Exception {
        String body = get("/events");
        assertTrue(body.contains("Timestamp"));
        assertTrue(body.contains("User"));
        assertTrue(body.contains("IP"));
        assertTrue(body.contains("Port"));
        assertTrue(body.contains("Service"));
        assertTrue(body.contains("Event"));
    }

    // ── AC3 — Currently active remote users page ──────────────────────────────

    @Test @Order(6)
    void activeUsersPageReturns200() throws Exception {
        assertEquals(200, status("/active-users"));
    }

    @Test @Order(7)
    void activeUsersPageHasAjaxRefresh() throws Exception {
        String body = get("/active-users");
        assertTrue(body.contains("fetch"));
        assertTrue(body.contains("setInterval"));
    }

    @Test @Order(8)
    void activeUsersPageContainsAllRequiredColumnHeaders() throws Exception {
        String body = get("/active-users");
        assertTrue(body.contains("Username"));
        assertTrue(body.contains("IP"));
        assertTrue(body.contains("Port"));
        assertTrue(body.contains("Service"));
    }

    // ── API endpoints start empty ─────────────────────────────────────────────

    @Test @Order(9)
    void apiEventsInitiallyEmptyJsonArray() throws Exception {
        assertEquals("[]", get("/api/events").trim());
    }

    @Test @Order(10)
    void apiActiveUsersInitiallyEmptyJsonArray() throws Exception {
        assertEquals("[]", get("/api/active-users").trim());
    }

    // ── AC5 — AJAX endpoints return structured JSON ───────────────────────────

    @Test @Order(11)
    void successfulLoginAppearsInEventsApi() throws Exception {
        udp(ts() + "|pilot@tap.pt|10.0.0.1|54321|US086|SUCCESSFUL_LOGIN");
        String events = get("/api/events");
        assertTrue(events.contains("pilot@tap.pt"));
        assertTrue(events.contains("SUCCESSFUL_LOGIN"));
        assertTrue(events.contains("US086"));
    }

    @Test @Order(12)
    void successfulLoginMakesUserActiveInActiveUsersApi() throws Exception {
        assertTrue(get("/api/active-users").contains("pilot@tap.pt"));
    }

    @Test @Order(13)
    void failedLoginAppearsInEventsButNotInActiveUsers() throws Exception {
        udp(ts() + "|bad@test.com|192.168.1.1|9000|US044|FAILED_LOGIN");
        assertTrue(get("/api/events").contains("bad@test.com"));
        assertFalse(get("/api/active-users").contains("bad@test.com"));
    }

    @Test @Order(14)
    void logoutEventRemovesUserFromActiveUsers() throws Exception {
        udp(ts() + "|pilot@tap.pt|10.0.0.1|54321|US086|LOGOUT");
        assertFalse(get("/api/active-users").contains("pilot@tap.pt"));
    }

    @Test @Order(15)
    void disconnectEventRemovesUserFromActiveUsers() throws Exception {
        udp(ts() + "|disco@test.com|10.1.0.1|8888|US078|SUCCESSFUL_LOGIN");
        Thread.sleep(100);
        udp(ts() + "|disco@test.com|10.1.0.1|8888|US078|DISCONNECT");
        assertFalse(get("/api/active-users").contains("disco@test.com"));
    }

    @Test @Order(16)
    void jsonContainsAllRequiredFieldNames() throws Exception {
        udp(ts() + "|fields@test.com|172.16.0.5|7777|US086|SUCCESSFUL_LOGIN");
        String body = get("/api/events");
        for (String field : List.of("timestamp", "username", "clientIp", "clientPort", "service", "eventType")) {
            assertTrue(body.contains("\"" + field + "\""), "Missing JSON field: " + field);
        }
    }

    @Test @Order(17)
    void apiResponsesAreValidJsonArrays() throws Exception {
        for (String path : List.of("/api/events", "/api/active-users")) {
            String body = get(path).trim();
            assertTrue(body.startsWith("[") && body.endsWith("]"), "Not a JSON array: " + path);
        }
    }

    @Test @Order(18)
    void allServicesAreAccepted() throws Exception {
        int before = count(get("/api/events"), "\"eventType\"");
        for (RemoteService svc : RemoteService.values()) {
            udp(ts() + "|svc@test.com|127.0.0.1|1111|" + svc.name() + "|FAILED_LOGIN");
        }
        Thread.sleep(200);
        int after = count(get("/api/events"), "\"eventType\"");
        assertEquals(before + RemoteService.values().length, after);
    }

    @Test @Order(19)
    void allEventTypesAreAccepted() throws Exception {
        udp(ts() + "|typestest@test.com|127.0.0.1|2222|US044|SUCCESSFUL_LOGIN");
        Thread.sleep(100);
        for (RemoteAccessEventType type : RemoteAccessEventType.values()) {
            udp(ts() + "|typestest@test.com|127.0.0.1|2222|US044|" + type.name());
        }
        Thread.sleep(200);
        String events = get("/api/events");
        for (RemoteAccessEventType type : RemoteAccessEventType.values()) {
            assertTrue(events.contains(type.name()), "Missing event type: " + type.name());
        }
    }

    // ── Log file persistence ──────────────────────────────────────────────────

    @Test @Order(20)
    void logFileIsWrittenToDisk() throws Exception {
        List<Path> logs = Files.list(logDir)
                .filter(p -> p.getFileName().toString().startsWith("remote-access-"))
                .toList();
        assertFalse(logs.isEmpty(), "No log file found in log directory");
        assertFalse(Files.readString(logs.get(0)).isBlank(), "Log file is empty");
    }

    // ── Duplicate detection ───────────────────────────────────────────────────

    @Test @Order(21)
    void identicalDatagramsWithinTwoSecondsAreDeduped() throws Exception {
        int before = count(get("/api/events"), "\"eventType\"");
        String payload = ts() + "|dup@test.com|127.0.0.1|3333|US044|FAILED_LOGIN";
        udp(payload);
        udp(payload);
        assertEquals(before + 1, count(get("/api/events"), "\"eventType\""));
    }

    // ── Malformed / error paths ───────────────────────────────────────────────

    @Test @Order(22)
    void malformedDatagramIsIgnoredAndDoesNotAddEvent() throws Exception {
        int before = count(get("/api/events"), "\"eventType\"");
        udp("only|three|parts");
        assertEquals(before, count(get("/api/events"), "\"eventType\""));
    }

    @Test @Order(23)
    void startExitsEarlyWhenLogDirIsAFile() throws Exception {
        Path file = Files.createTempFile("not-a-dir", ".tmp");
        Thread t = new Thread(new RemoteAccessesLoggingServer(29998, 28098, file)::start);
        t.setDaemon(true);
        t.start();
        t.join(2000);
        assertFalse(t.isAlive(), "Thread should exit early when log dir cannot be created");
        Files.deleteIfExists(file);
    }

    @Test @Order(24)
    void httpServerFailsGracefullyWhenPortIsOccupied() throws Exception {
        Path dir2 = Files.createTempDirectory("aisafe-log-test2-");
        Thread t = new Thread(new RemoteAccessesLoggingServer(29997, HTTP, dir2)::start);
        t.setDaemon(true);
        t.start();
        Thread.sleep(600);
        assertTrue(t.isAlive(), "UDP loop should still run even if HTTP port is occupied");
        Files.walk(dir2).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    // ── main() and resolve methods ────────────────────────────────────────────

    @Test @Order(25)
    void mainStartsServerUsingSystemProperties() throws Exception {
        System.setProperty("aisafe.logging.port",      "29996");
        System.setProperty("aisafe.logging.http.port", "28096");
        System.setProperty("aisafe.logging.dir",       logDir.toString());
        Thread t = new Thread(() -> RemoteAccessesLoggingServer.main(new String[0]));
        t.setDaemon(true);
        t.start();
        Thread.sleep(600);
        int code = ((HttpURLConnection) new URL("http://localhost:28096/").openConnection()).getResponseCode();
        assertEquals(200, code);
        System.clearProperty("aisafe.logging.port");
        System.clearProperty("aisafe.logging.http.port");
        System.clearProperty("aisafe.logging.dir");
    }

    @Test @Order(26)
    void resolveUdpPortReadsSystemProperty() throws Exception {
        System.setProperty("aisafe.logging.port", "12345");
        Method m = RemoteAccessesLoggingServer.class.getDeclaredMethod("resolveUdpPort");
        m.setAccessible(true);
        assertEquals(12345, m.invoke(null));
        System.clearProperty("aisafe.logging.port");
    }

    @Test @Order(27)
    void resolveUdpPortFallsBackToDefaultOnInvalidValue() throws Exception {
        System.setProperty("aisafe.logging.port", "not-a-number");
        Method m = RemoteAccessesLoggingServer.class.getDeclaredMethod("resolveUdpPort");
        m.setAccessible(true);
        assertEquals(9999, m.invoke(null));
        System.clearProperty("aisafe.logging.port");
    }

    @Test @Order(28)
    void resolveHttpPortReadsSystemProperty() throws Exception {
        System.setProperty("aisafe.logging.http.port", "8888");
        Method m = RemoteAccessesLoggingServer.class.getDeclaredMethod("resolveHttpPort");
        m.setAccessible(true);
        assertEquals(8888, m.invoke(null));
        System.clearProperty("aisafe.logging.http.port");
    }

    @Test @Order(29)
    void resolveHttpPortFallsBackToDefaultOnInvalidValue() throws Exception {
        System.setProperty("aisafe.logging.http.port", "not-a-number");
        Method m = RemoteAccessesLoggingServer.class.getDeclaredMethod("resolveHttpPort");
        m.setAccessible(true);
        assertEquals(8080, m.invoke(null));
        System.clearProperty("aisafe.logging.http.port");
    }

    @Test @Order(30)
    void resolveDirReturnsDefaultWhenNoPropertySet() throws Exception {
        Method m = RemoteAccessesLoggingServer.class.getDeclaredMethod("resolveDir");
        m.setAccessible(true);
        assertEquals("logs", m.invoke(null));
    }

    @Test @Order(31)
    void resolveDirReadsSystemProperty() throws Exception {
        System.setProperty("aisafe.logging.dir", "/tmp/custom-logs");
        Method m = RemoteAccessesLoggingServer.class.getDeclaredMethod("resolveDir");
        m.setAccessible(true);
        assertEquals("/tmp/custom-logs", m.invoke(null));
        System.clearProperty("aisafe.logging.dir");
    }
}
