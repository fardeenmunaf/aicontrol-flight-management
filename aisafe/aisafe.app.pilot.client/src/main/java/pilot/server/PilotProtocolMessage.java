package pilot.server;

import java.nio.charset.StandardCharsets;

public class PilotProtocolMessage {
    public static final byte VERSION = 1;

    private final PilotMessageCode code;
    private final byte[] data;

    public PilotProtocolMessage(final PilotMessageCode code, final byte[] data) {
        this.code = code;
        this.data = data;
    }

    public PilotProtocolMessage(final PilotMessageCode code, final String data) {
        this.code = code;
        this.data = data.getBytes(StandardCharsets.UTF_8);
    }

    public PilotMessageCode code() { return code; }
    public byte[] data() { return data; }
    public String dataAsString() { return new String(data, StandardCharsets.UTF_8); }
}
