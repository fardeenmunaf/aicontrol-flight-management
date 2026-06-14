package airtransport.presentation.flightroute;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class DeleteFlightRouteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listFlightRoutes();
            if (raw.isBlank()) {
                System.out.println("No flight routes found for your company.");
                return true;
            }

            // routes CSV: id;designation;originIata;destIata;activeFrom;...;isActive
            final List<String[]> active = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                if ("true".equalsIgnoreCase(f[f.length - 1])) active.add(f);
            }

            if (active.isEmpty()) {
                System.out.println("No active flight routes found for your company.");
                return true;
            }

            System.out.println("Active flight routes:");
            for (int i = 0; i < active.size(); i++) {
                final String[] f = active.get(i);
                System.out.printf("%d) %s  [%s -> %s]  Active from: %s%n",
                        i + 1, f[1], f[2], f[3], f[4]);
            }

            final String choiceStr = Console.readLine("Select route number to deactivate");
            if (choiceStr == null) return true;
            int choice;
            try { choice = Integer.parseInt(choiceStr.trim()); }
            catch (final NumberFormatException e) { System.out.println("Invalid selection."); return true; }

            if (choice < 1 || choice > active.size()) {
                System.out.println("Invalid selection.");
                return true;
            }

            final String[] selected = active.get(choice - 1);
            final String dateStr = Console.readLine("Deactivation date (YYYY-MM-DD)");
            if (dateStr == null || dateStr.isBlank()) {
                System.out.println("Date cannot be empty.");
                return true;
            }

            final String confirm = Console.readLine(
                    "Deactivate route " + selected[1] + " from " + dateStr.trim() + "? (yes/no)");
            if (confirm == null || !confirm.trim().equalsIgnoreCase("yes")) {
                System.out.println("Cancelled.");
                return true;
            }

            // f[0] = routeId
            System.out.println(AirTransportContext.service().deleteFlightRoute(selected[0], dateStr.trim()));
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "Deactivate Flight Route"; }
}
