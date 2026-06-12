package pilot.presentation.fleet;

import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;

import java.io.IOException;

public class ListFleetRemoteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String response = PilotClientSession.getClient().listFleet();
            if (response == null || response.isBlank()) {
                System.out.println("  No certified aircraft found in your company fleet.");
                return false;
            }
            System.out.println("\n  Company Fleet (aircraft you are certified for):");
            System.out.printf("  %-15s %-25s %-20s %s%n",
                    "Registration", "Model", "Manufacturer", "Status");
            System.out.println("  " + "-".repeat(75));
            for (final String line : response.split("\n")) {
                if (line.isBlank()) continue;
                final String[] p = line.split(";", -1);
                System.out.printf("  %-15s %-25s %-20s %s%n",
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
        return "My Certified Fleet";
    }
}
