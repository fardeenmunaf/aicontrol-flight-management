package pilot.server;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * US086 — Pilot TCP Server.
 * Listens for remote pilot connections and dispatches each to AttendPilotClient.
 * Port is configurable via system property aisafe.pilot.port (default: 9001).
 */
public class PilotTcpServer {

    private static final int DEFAULT_PORT = 9001;

    public static void main(final String[] args) {
        System.out.println("Starting Pilot TCP Server (US086)...");
        try {
            AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                    PersistenceSettings.getPersistenceUnitName(),
                    PersistenceSettings.getExtendedPersistenceProperties()));
            System.out.println("Connecting to database...");
            infrastructure.persistence.PersistenceContext.repositories().users();
            System.out.println("Database connection established.");
        } catch (final Exception e) {
            System.err.println("CRITICAL ERROR: Could not initialize database connection.");
            System.err.println("Make sure your database server is running.");
            System.err.println("Error details: " + e.getMessage());
            return;
        }

        final int port = resolvePort();
        try (ServerSocket sock = new ServerSocket(port)) {
            System.out.println("Pilot TCP Server listening on 0.0.0.0:" + port);
            System.out.println("Pilots can now connect remotely. (Ctrl+C to stop)");

            while (true) {
                final Socket nSock = sock.accept();
                System.out.println("[PilotServer] New connection from " + nSock.getRemoteSocketAddress());
                final Thread t = new Thread(new AttendPilotClient(nSock));
                t.setDaemon(true);
                t.start();
            }
        } catch (final IOException e) {
            System.err.println("Could not listen on port " + port + ": " + e.getMessage());
        }
    }

    private static int resolvePort() {
        try {
            final String prop = System.getProperty("aisafe.pilot.port");
            if (prop != null && !prop.isBlank()) return Integer.parseInt(prop);
        } catch (final NumberFormatException ignored) {}
        return DEFAULT_PORT;
    }
}
