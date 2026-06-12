package pilot.presentation.flightroute;

import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;

import java.io.IOException;

public class ListFlightRoutesRemoteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String response = PilotClientSession.getClient().listRoutes();
            if (response == null || response.isBlank()) {
                System.out.println("  No routes found for your company.");
                return false;
            }
            System.out.println("\n  Company Flight Routes:");
            System.out.printf("  %-15s %-6s %-6s %s%n",
                    "Designation", "From", "To", "Active From");
            System.out.println("  " + "-".repeat(50));
            for (final String line : response.split("\n")) {
                if (line.isBlank()) continue;
                final String[] p = line.split(";", -1);
                System.out.printf("  %-15s %-6s %-6s %s%n",
                        p.length > 0 ? p[0] : "?",
                        p.length > 1 ? p[1] : "?",
                        p.length > 2 ? p[2] : "?",
                        p.length > 3 ? p[3] : "?");
            }
        } catch (final IOException e) {
            System.out.println("  Error communicating with server: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Company Flight Routes";
    }
}
