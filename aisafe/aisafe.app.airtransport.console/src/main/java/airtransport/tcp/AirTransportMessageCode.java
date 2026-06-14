package airtransport.tcp;

public enum AirTransportMessageCode {
    COMMTEST((byte) 0),
    DISCONN((byte) 1),
    ACK((byte) 2),
    ERR((byte) 3),
    AUTH((byte) 4),

    LIST_AIRCRAFT_MODELS((byte) 20),
    LIST_FLEET((byte) 21),
    REGISTER_AIRCRAFT((byte) 22),
    DECOMMISSION_AIRCRAFT((byte) 23),

    LIST_AIRPORTS((byte) 24),
    LIST_FLIGHT_ROUTES((byte) 25),
    CREATE_FLIGHT_ROUTE((byte) 26),
    DELETE_FLIGHT_ROUTE((byte) 27),

    LIST_FLIGHT_PLANS((byte) 28),
    CREATE_FLIGHT_PLAN((byte) 29),
    VALIDATE_FLIGHT_PLAN((byte) 30),
    ADD_WEATHER_DATA((byte) 31),

    LIST_COMPANY_PILOTS((byte) 32),
    LIST_AVAILABLE_PILOTS((byte) 33),
    ADD_PILOT((byte) 34),
    REMOVE_PILOT((byte) 35),
    ACTIVATE_PILOT((byte) 36),
    DEACTIVATE_PILOT((byte) 37);

    private final byte code;

    AirTransportMessageCode(final byte code) { this.code = code; }

    public byte code() { return code; }

    public static AirTransportMessageCode from(final byte code) {
        for (final AirTransportMessageCode mc : values()) {
            if (mc.code == code) return mc;
        }
        throw new IllegalArgumentException("Unknown air transport message code: " + code);
    }
}
