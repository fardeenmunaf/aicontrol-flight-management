package remotelogging.domain;

import java.time.Instant;
import java.util.Objects;

public class RemoteAccessEvent {

    private static final String DELIMITER = "|";

    private final Instant timestamp;
    private final String username;
    private final String clientIp;
    private final int clientPort;
    private final RemoteService service;
    private final RemoteAccessEventType eventType;

    public RemoteAccessEvent(final String username,
                              final String clientIp,
                              final int clientPort,
                              final RemoteService service,
                              final RemoteAccessEventType eventType) {
        this.timestamp  = Instant.now();
        this.username   = Objects.requireNonNull(username,  "username");
        this.clientIp   = Objects.requireNonNull(clientIp,  "clientIp");
        this.clientPort = clientPort;
        this.service    = Objects.requireNonNull(service,   "service");
        this.eventType  = Objects.requireNonNull(eventType, "eventType");
    }

    public String serialize() {
        return timestamp.toString()
                + DELIMITER + username
                + DELIMITER + clientIp
                + DELIMITER + clientPort
                + DELIMITER + service.name()
                + DELIMITER + eventType.name();
    }

    public static RemoteAccessEvent deserialize(final String raw) {
        final String[] parts = raw.split("\\|", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid log entry: " + raw);
        }
        final RemoteAccessEvent event = new RemoteAccessEvent(
                parts[1],
                parts[2],
                Integer.parseInt(parts[3]),
                RemoteService.valueOf(parts[4]),
                RemoteAccessEventType.valueOf(parts[5]));
        return event;
    }

    public Instant timestamp()          { return timestamp; }
    public String username()            { return username; }
    public String clientIp()            { return clientIp; }
    public int clientPort()             { return clientPort; }
    public RemoteService service()      { return service; }
    public RemoteAccessEventType eventType() { return eventType; }

    @Override
    public String toString() {
        return String.format("[%s] %s | user=%s | %s:%d | %s",
                eventType, service, username, clientIp, clientPort, timestamp);
    }
}
