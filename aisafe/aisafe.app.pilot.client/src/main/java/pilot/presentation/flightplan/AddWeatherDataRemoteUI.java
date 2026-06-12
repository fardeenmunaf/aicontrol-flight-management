package pilot.presentation.flightplan;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddWeatherDataRemoteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            System.out.println("  Fetching your flight plans...");
            final String plans = PilotClientSession.getClient().listMyPlans();
            if (plans == null || plans.isBlank()) {
                System.out.println("  You have no flight plans.");
                return false;
            }

            final List<String[]> rows = new ArrayList<>();
            for (final String line : plans.split("\n")) {
                if (!line.isBlank()) rows.add(line.split(";", -1));
            }

            System.out.println("\n  Your Flight Plans:");
            for (int i = 0; i < rows.size(); i++) {
                final String[] p = rows.get(i);
                System.out.printf("  %d. %-20s [%s] %s -> %s%n",
                        i + 1,
                        p.length > 0 ? p[0] : "?",
                        p.length > 1 ? p[1] : "?",
                        p.length > 2 ? p[2] : "?",
                        p.length > 3 ? p[3] : "?");
            }

            final String input = Console.readLine("  Select number to add weather data (0 to cancel)");
            if (input == null || "0".equals(input.trim())) {
                System.out.println("  Cancelled.");
                return false;
            }

            final int choice;
            try {
                choice = Integer.parseInt(input.trim());
            } catch (final NumberFormatException e) {
                System.out.println("  Invalid selection.");
                return false;
            }

            if (choice < 1 || choice > rows.size()) {
                System.out.println("  Invalid selection.");
                return false;
            }

            final String[] selected = rows.get(choice - 1);
            final String planId = selected.length > 5 ? selected[5] : "";
            if (planId.isBlank()) {
                System.out.println("  Could not determine plan ID.");
                return false;
            }

            System.out.println("  Searching for relevant weather data on server...");
            final String response = PilotClientSession.getClient().addWeather(planId);
            System.out.println("  " + response);
        } catch (final IOException e) {
            System.out.println("  Error communicating with server: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Add Weather Data to Flight Plan (Remote)";
    }
}
