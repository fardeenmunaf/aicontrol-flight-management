package pilot.presentation.authz;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;
import pilot.server.PilotTcpClient;

import java.io.IOException;

public class PilotLoginUI extends AbstractUI {

    private final String host;
    private final int    port;

    public PilotLoginUI(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected boolean doShow() {
        System.out.println("Connecting to Pilot TCP Server at " + host + ":" + port + " ...");
        boolean authenticated = false;
        while (!authenticated) {
            final String email    = Console.readLine("Email");
            final String password = Console.readLine("Password");
            try {
                final PilotTcpClient client = new PilotTcpClient(host, port);
                if (client.login(email, password)) {
                    PilotClientSession.setClient(client);
                    authenticated = true;
                    System.out.println("Login successful. Welcome, Pilot!");
                } else {
                    client.close();
                    System.out.println("Invalid credentials or insufficient role. Please try again.");
                }
            } catch (final IOException e) {
                System.out.println("Could not connect to server: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public String headline() {
        return "Pilot Remote Login";
    }
}
