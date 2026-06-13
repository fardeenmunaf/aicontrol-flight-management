package pilot.server;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PilotProtocolMessageTest {

    @Test
    void ensureVersionConstantIsOne() {
        assertEquals(1, PilotProtocolMessage.VERSION);
    }

    @Test
    void ensureConstructionWithByteArrayReturnsExpectedCode() {
        final byte[] data = new byte[]{1, 2, 3};
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, data);
        assertEquals(PilotMessageCode.ACK, msg.code());
    }

    @Test
    void ensureConstructionWithByteArrayReturnsExpectedData() {
        final byte[] data = new byte[]{10, 20, 30};
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, data);
        assertArrayEquals(data, msg.data());
    }

    @Test
    void ensureConstructionWithStringEncodesAsUtf8() {
        final String text = "hello";
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, text);
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), msg.data());
    }

    @Test
    void ensureDataAsStringRoundtrip() {
        final String original = "Authenticated as Pilot";
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, original);
        assertEquals(original, msg.dataAsString());
    }

    @Test
    void ensureEmptyStringDataIsAccepted() {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.COMMTEST, "");
        assertEquals("", msg.dataAsString());
        assertEquals(0, msg.data().length);
    }

    @Test
    void ensureEmptyByteArrayDataIsAccepted() {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.DISCONN, new byte[0]);
        assertEquals(0, msg.data().length);
    }

    @Test
    void ensureAuthMessageWithNullBytePayload() {
        final String payload = "pilot@test.com\0secret";
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.AUTH, payload);
        assertEquals(PilotMessageCode.AUTH, msg.code());
        assertEquals(payload, msg.dataAsString());
    }

    @Test
    void ensureErrCodeIsPreserved() {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ERR, "Not authorized");
        assertEquals(PilotMessageCode.ERR, msg.code());
    }
}
