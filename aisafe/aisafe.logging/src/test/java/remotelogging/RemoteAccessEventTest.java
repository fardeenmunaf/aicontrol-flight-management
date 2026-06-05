package remotelogging;

import org.junit.jupiter.api.Test;
import remotelogging.domain.RemoteAccessEvent;
import remotelogging.domain.RemoteAccessEventType;
import remotelogging.domain.RemoteService;

import static org.junit.jupiter.api.Assertions.*;

class RemoteAccessEventTest {

    @Test
    void ensureEventIsCreatedWithExpectedFields() {
        final RemoteAccessEvent event = new RemoteAccessEvent(
                "wp@aisafe.com", "192.168.1.10", 54321, RemoteService.US044, RemoteAccessEventType.SUCCESSFUL_LOGIN);

        assertEquals("wp@aisafe.com", event.username());
        assertEquals("192.168.1.10", event.clientIp());
        assertEquals(54321, event.clientPort());
        assertEquals(RemoteService.US044, event.service());
        assertEquals(RemoteAccessEventType.SUCCESSFUL_LOGIN, event.eventType());
        assertNotNull(event.timestamp());
    }

    @Test
    void ensureSerializationContainsAllFields() {
        final RemoteAccessEvent event = new RemoteAccessEvent(
                "wp@aisafe.com", "192.168.1.10", 54321, RemoteService.US044, RemoteAccessEventType.SUCCESSFUL_LOGIN);

        final String serialized = event.serialize();

        assertTrue(serialized.contains("wp@aisafe.com"));
        assertTrue(serialized.contains("192.168.1.10"));
        assertTrue(serialized.contains("54321"));
        assertTrue(serialized.contains("US044"));
        assertTrue(serialized.contains("SUCCESSFUL_LOGIN"));
    }

    @Test
    void ensureDeserializationRoundTrip() {
        final RemoteAccessEvent original = new RemoteAccessEvent(
                "pilot@tap.pt", "10.0.0.5", 9876, RemoteService.US086, RemoteAccessEventType.LOGOUT);

        final String serialized = original.serialize();
        final RemoteAccessEvent restored = RemoteAccessEvent.deserialize(serialized);

        assertEquals(original.username(), restored.username());
        assertEquals(original.clientIp(), restored.clientIp());
        assertEquals(original.clientPort(), restored.clientPort());
        assertEquals(original.service(), restored.service());
        assertEquals(original.eventType(), restored.eventType());
    }

    @Test
    void ensureDeserializationFailsOnMalformedInput() {
        assertThrows(IllegalArgumentException.class,
                () -> RemoteAccessEvent.deserialize("malformed|data"));
    }

    @Test
    void ensureNullUsernameIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new RemoteAccessEvent(null, "192.168.1.1", 1234, RemoteService.US044, RemoteAccessEventType.FAILED_LOGIN));
    }

    @Test
    void ensureAllServicesAreSupported() {
        for (final RemoteService service : RemoteService.values()) {
            final RemoteAccessEvent event = new RemoteAccessEvent(
                    "user@test.com", "127.0.0.1", 1000, service, RemoteAccessEventType.DISCONNECT);
            assertNotNull(event.serialize());
        }
    }

    @Test
    void ensureAllEventTypesAreSupported() {
        for (final RemoteAccessEventType type : RemoteAccessEventType.values()) {
            final RemoteAccessEvent event = new RemoteAccessEvent(
                    "user@test.com", "127.0.0.1", 1000, RemoteService.US078, type);
            assertNotNull(event.serialize());
        }
    }

    @Test
    void ensureToStringContainsKeyInfo() {
        final RemoteAccessEvent event = new RemoteAccessEvent(
                "collab@company.com", "10.10.10.1", 8080, RemoteService.US078, RemoteAccessEventType.FAILED_LOGIN);

        final String str = event.toString();
        assertTrue(str.contains("collab@company.com"));
        assertTrue(str.contains("US078"));
        assertTrue(str.contains("FAILED_LOGIN"));
    }
}
