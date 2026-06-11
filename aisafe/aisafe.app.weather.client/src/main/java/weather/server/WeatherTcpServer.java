package weather.server;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WeatherTcpServer {
    public static void main(String[] args) {
        System.out.println("Starting Weather TCP Server...");
        try {
            // Bootstrap do EAPLI (Spring + JPA)
            AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                    PersistenceSettings.getPersistenceUnitName(),
                    PersistenceSettings.getExtendedPersistenceProperties()));
            
            // Test connection by accessing repositories
            System.out.println("Connecting to database...");
            infrastructure.persistence.PersistenceContext.repositories().users();
            System.out.println("Database connection established.");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Could not initialize database connection.");
            System.err.println("Make sure your database server is running (e.g., run start-h2-server.bat).");
            System.err.println("Error details: " + e.getMessage());
            return;
        }

        try (ServerSocket sock = new ServerSocket(9000)) {
            System.out.println("Weather TCP Server listening on 0.0.0.0:9000");

            while (true) {
                Socket nSock = sock.accept();
                System.out.println("New connection from " + nSock.getRemoteSocketAddress());
                Thread cliConn = new Thread(new AttendClient(nSock));
                cliConn.start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port 9000: " + e.getMessage());
        }
    }
}