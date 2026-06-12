package pilot.presentation.flightplan;

import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;

import java.io.IOException;

public class ListFlightPlansUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String response = PilotClientSession.getClient().listMyPlans();
            if (response == null || response.isBlank()) {
                System.out.println("  You have no flight plans.");
                return false;
            }
            System.out.println("\n  Your Flight Plans:");
            System.out.printf("  %-20s %-10s %-6s %-6s %s%n",
                    "Designator", "Status", "From", "To", "Departure");
            System.out.println("  " + "-".repeat(70));
            for (final String line : response.split("\n")) {
                if (line.isBlank()) continue;
                final String[] p = line.split(";", -1);
                if (p.length >= 5) {
                    System.out.printf("  %-20s %-10s %-6s %-6s %s%n",
                            p[0], p[1], p[2], p[3], p[4]);
                }
            }
        } catch (final IOException e) {
            System.out.println("  Error communicating with server: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "My Flight Plans";
    }
}
