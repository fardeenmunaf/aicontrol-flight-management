package airtransport.presentation.flightroute;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class CreateFlightRouteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String airportRaw = AirTransportContext.service().listAirports();
            if (airportRaw.isBlank()) {
                System.out.println("No airports in the system. Cannot create a route.");
                return true;
            }

            // airports CSV: iataCode;name;town;country  (sorted below)
            final List<String[]> airports = new ArrayList<>();
            for (final String line : airportRaw.split("\n")) {
                airports.add(line.split(";", -1));
            }
            airports.sort((a, b) -> a[0].compareTo(b[0]));

            System.out.println();
            System.out.printf("  %-4s %-6s %-25s %-15s %s%n", "No.", "IATA", "Name", "Town", "Country");
            System.out.println("  " + "-".repeat(72));
            for (int i = 0; i < airports.size(); i++) {
                final String[] a = airports.get(i);
                System.out.printf("  %-4d %-6s %-25s %-15s %s%n", i + 1, a[0], a[1], a[2], a[3]);
            }
            System.out.println();

            final String[] origin = selectAirport(airports, "Select origin airport number");
            if (origin == null) return true;
            final String[] destination = selectAirport(airports, "Select destination airport number");
            if (destination == null) return true;

            final String suffix = Console.readLine("Enter numeric suffix for route designation (e.g. 123)");
            if (suffix == null || suffix.trim().isEmpty()) {
                System.out.println("Suffix cannot be empty.");
                return true;
            }

            final String activeDateStr = Console.readLine("Active from date (YYYY-MM-DD, leave blank for today)");

            // Designation: we don't know iataCode here, so we delegate to service (it'll use the stored company iata)
            // But we need to ask for the full designation or build it. The service expects the full designation.
            // Let's ask the user for the full designation since we don't have company iataCode on the client side.
            // In local mode: LocalAirTransportService already has companyIcaoCode.
            // We build it: ask for the prefix or just let user enter the designation suffix.
            // We'll prompt for the full designation instead to keep it simple.
            final String fullDesignation = Console.readLine("Full route designation (e.g. TP123)");
            if (fullDesignation == null || fullDesignation.trim().isEmpty()) {
                System.out.println("Designation cannot be empty.");
                return true;
            }

            final String result = AirTransportContext.service().createFlightRoute(
                    fullDesignation.trim(), origin[0], destination[0],
                    activeDateStr == null ? "" : activeDateStr.trim());
            System.out.println(result);
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    private static String[] selectAirport(final List<String[]> airports, final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) return null;
        try {
            final int choice = Integer.parseInt(raw.trim());
            if (choice < 1 || choice > airports.size()) {
                System.out.println("Invalid selection.");
                return null;
            }
            return airports.get(choice - 1);
        } catch (final NumberFormatException e) {
            System.out.println("Invalid number.");
            return null;
        }
    }

    @Override
    public String headline() { return "Create Flight Route"; }
}
