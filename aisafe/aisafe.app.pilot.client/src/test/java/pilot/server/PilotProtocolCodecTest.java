package pilot.server;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class PilotProtocolCodecTest {

    private byte[] encode(final PilotProtocolMessage msg) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PilotProtocolCodec.write(new DataOutputStream(baos), msg);
        return baos.toByteArray();
    }

    private PilotProtocolMessage decode(final byte[] bytes) throws IOException {
        return PilotProtocolCodec.read(new DataInputStream(new ByteArrayInputStream(bytes)));
    }

    @Test
    void ensureRoundtripWithEmptyPayload() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.COMMTEST, new byte[0]);
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.COMMTEST, decoded.code());
        assertEquals(0, decoded.data().length);
    }

    @Test
    void ensureRoundtripWithStringPayload() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, "Authenticated as Pilot");
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.ACK, decoded.code());
        assertEquals("Authenticated as Pilot", decoded.dataAsString());
    }

    @Test
    void ensureRoundtripWithAuthPayload() throws IOException {
        final String payload = "pilot@test.com\0secret123";
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.AUTH, payload);
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.AUTH, decoded.code());
        assertEquals(payload, decoded.dataAsString());
    }

    @Test
    void ensureRoundtripForErrMessage() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ERR, "Invalid credentials");
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.ERR, decoded.code());
        assertEquals("Invalid credentials", decoded.dataAsString());
    }

    @Test
    void ensureRoundtripForDisconnMessage() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.DISCONN, new byte[0]);
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.DISCONN, decoded.code());
    }

    @Test
    void ensureRoundtripForListMyPlans() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.LIST_MY_PLANS, "TAP001\nTAP002");
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.LIST_MY_PLANS, decoded.code());
        assertEquals("TAP001\nTAP002", decoded.dataAsString());
    }

    @Test
    void ensureEncodedFrameHasCorrectVersionByte() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.COMMTEST, new byte[0]);
        final byte[] frame = encode(msg);
        assertEquals(PilotProtocolMessage.VERSION, frame[0]);
    }

    @Test
    void ensureEncodedFrameHasCorrectCodeByte() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.AUTH, new byte[0]);
        final byte[] frame = encode(msg);
        assertEquals(PilotMessageCode.AUTH.code(), frame[1]);
    }

    @Test
    void ensureEncodedFrameLengthFieldsAreCorrect() throws IOException {
        final byte[] payload = "hello".getBytes();
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, payload);
        final byte[] frame = encode(msg);
        final int lenLow  = frame[2] & 0xFF;
        final int lenHigh = frame[3] & 0xFF;
        assertEquals(payload.length, (lenHigh << 8) | lenLow);
    }

    @Test
    void ensureReadOnClosedStreamThrowsEOFException() {
        final byte[] empty = new byte[0];
        assertThrows(EOFException.class,
                () -> PilotProtocolCodec.read(new DataInputStream(new ByteArrayInputStream(empty))));
    }

    @Test
    void ensureReadTruncatedPayloadThrowsEOFException() throws IOException {
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.ACK, "hello world");
        final byte[] frame = encode(msg);
        final byte[] truncated = new byte[frame.length - 3];
        System.arraycopy(frame, 0, truncated, 0, truncated.length);
        assertThrows(EOFException.class,
                () -> PilotProtocolCodec.read(new DataInputStream(new ByteArrayInputStream(truncated))));
    }

    @Test
    void ensureRoundtripWithLargePayload() throws IOException {
        final String large = "A".repeat(500);
        final PilotProtocolMessage msg = new PilotProtocolMessage(PilotMessageCode.CREATE_PLAN, large);
        final PilotProtocolMessage decoded = decode(encode(msg));
        assertEquals(PilotMessageCode.CREATE_PLAN, decoded.code());
        assertEquals(large, decoded.dataAsString());
    }
}
