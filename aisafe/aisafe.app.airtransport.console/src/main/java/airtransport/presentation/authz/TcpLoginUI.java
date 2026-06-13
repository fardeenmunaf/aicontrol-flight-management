package airtransport.presentation.authz;

import airtransport.AirTransportContext;
import airtransport.service.TcpAirTransportService;
import airtransport.tcp.AirTransportTcpClient;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class TcpLoginUI extends AbstractUI {

    private final String host;
    private final int    port;

    public TcpLoginUI(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected boolean doShow() {
        while (true) {
            final String email    = Console.readLine("Email");
            final String password = Console.readLine("Password");

            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                System.out.println("Email and password cannot be empty.");
                continue;
            }

            try {
                final AirTransportTcpClient client = new AirTransportTcpClient(host, port);
                if (client.login(email.trim(), password.trim())) {
                    AirTransportContext.setService(new TcpAirTransportService(client));
                    System.out.println("Authenticated.");
                    return true;
                } else {
                    client.close();
                    System.out.println("Invalid credentials. Please try again.");
                }
            } catch (final Exception e) {
                System.out.println("Connection error: " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public String headline() {
        return "Remote Login";
    }
}
