package weather.server;

public enum MessageCode {
    COMMTEST((byte) 0),
    DISCONN((byte) 1),
    ACK((byte) 2),
    ERR((byte) 3),
    AUTH((byte) 4),
    UPLOAD_WEATHER((byte) 10),
    LIST_WEATHER((byte) 11);

    private final byte code;

    MessageCode(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static MessageCode from(byte code) {
        for (MessageCode mc : MessageCode.values()) {
            if (mc.code == code) return mc;
        }
        throw new IllegalArgumentException("Unknown message code: " + code);
    }
}
