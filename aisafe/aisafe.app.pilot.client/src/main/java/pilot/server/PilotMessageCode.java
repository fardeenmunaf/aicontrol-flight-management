package pilot.server;

public enum PilotMessageCode {
    COMMTEST((byte) 0),
    DISCONN((byte) 1),
    ACK((byte) 2),
    ERR((byte) 3),
    AUTH((byte) 4),
    LIST_MY_PLANS((byte) 20),
    CREATE_PLAN((byte) 21),
    VALIDATE_PLAN((byte) 22),
    ADD_WEATHER((byte) 23),
    LIST_FLEET((byte) 24),
    LIST_ROUTES((byte) 25);

    private final byte code;

    PilotMessageCode(final byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static PilotMessageCode from(final byte code) {
        for (final PilotMessageCode mc : values()) {
            if (mc.code == code) return mc;
        }
        throw new IllegalArgumentException("Unknown pilot message code: " + code);
    }
}
