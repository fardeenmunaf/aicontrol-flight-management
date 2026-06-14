package airtransport.tcp;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class AirTransportTcpServer {

    private static final int DEFAULT_PORT = 9002;

    public static void start(final int port) {
        System.out.println("Starting Air Transport TCP Server (US078)...");
        try {
            AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                    PersistenceSettings.getPersistenceUnitName(),
                    PersistenceSettings.getExtendedPersistenceProperties()));
            System.out.println("Connecting to database...");
            infrastructure.persistence.PersistenceContext.repositories().users();
            System.out.println("Database connection established.");
        } catch (final Exception e) {
            System.err.println("CRITICAL: Could not initialise database: " + e.getMessage());
            return;
        }

        final int listenPort = port > 0 ? port : DEFAULT_PORT;
        try (ServerSocket sock = new ServerSocket(listenPort)) {
            System.out.println("Air Transport TCP Server listening on 0.0.0.0:" + listenPort);
            System.out.println("ATCC users can now connect remotely. (Ctrl+C to stop)");

            while (true) {
                final Socket nSock = sock.accept();
                System.out.println("[ATCServer] New connection from " + nSock.getRemoteSocketAddress());
                final Thread t = new Thread(new AttendAirTransportClient(nSock));
                t.setDaemon(true);
                t.start();
            }
        } catch (final IOException e) {
            System.err.println("Could not listen on port " + listenPort + ": " + e.getMessage());
        }
    }

    private AirTransportTcpServer() {}
}
