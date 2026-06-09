package weather.server;

import java.nio.charset.StandardCharsets;

public class ProtocolMessage {
    public static final byte VERSION = 1;

    private final MessageCode code;
    private final byte[] data;

    public ProtocolMessage(MessageCode code, byte[] data) {
        this.code = code;
        this.data = data;
    }

    public ProtocolMessage(MessageCode code, String data) {
        this.code = code;
        this.data = data.getBytes(StandardCharsets.UTF_8);
    }

    public MessageCode code() {
        return code;
    }

    public byte[] data() {
        return data;
    }

    public String dataAsString() {
        return new String(data, StandardCharsets.UTF_8);
    }
}
