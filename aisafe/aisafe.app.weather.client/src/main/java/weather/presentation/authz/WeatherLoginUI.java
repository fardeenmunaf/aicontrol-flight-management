package weather.presentation.authz;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import weather.ClientSession;
import weather.server.WeatherTcpClient;

import java.io.IOException;

public class WeatherLoginUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        boolean authenticated = false;
        while (!authenticated) {
            final String email = Console.readLine("Email");
            final String password = Console.readLine("Password");

            if (ClientSession.getMode() == ClientSession.ConnectionMode.DIRECT_DB) {
                authenticated = AuthzRegistry.authenticationService()
                        .authenticate(email, password)
                        .isPresent();
                if (!authenticated) {
                    System.out.println("Invalid credentials. Please try again.");
                }
            } else {
                try {
                    // In a real scenario, host/port should be in a config file
                    WeatherTcpClient client = new WeatherTcpClient("localhost", 9000);
                    if (client.login(email, password)) {
                        ClientSession.setClient(client);
                        authenticated = true;
                    } else {
                        client.close();
                        System.out.println("Invalid credentials. Please try again.");
                    }
                } catch (IOException e) {
                    System.out.println("Could not connect to server: " + e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String headline() {
        return "Login";
    }
}
