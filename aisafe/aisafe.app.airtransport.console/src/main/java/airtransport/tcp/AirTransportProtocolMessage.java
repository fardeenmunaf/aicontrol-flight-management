package airtransport.tcp;

import java.nio.charset.StandardCharsets;

public class AirTransportProtocolMessage {

    public static final byte VERSION = 1;

    private final AirTransportMessageCode code;
    private final byte[] data;

    public AirTransportProtocolMessage(final AirTransportMessageCode code, final byte[] data) {
        this.code = code;
        this.data = data;
    }

    public AirTransportProtocolMessage(final AirTransportMessageCode code, final String data) {
        this.code = code;
        this.data = data.getBytes(StandardCharsets.UTF_8);
    }

    public AirTransportMessageCode code() { return code; }
    public byte[] data() { return data; }
    public String dataAsString() { return new String(data, StandardCharsets.UTF_8); }
}
