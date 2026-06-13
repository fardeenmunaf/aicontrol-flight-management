package airtransport.presentation.pilot;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class RemovePilotFromCompanyUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listCompanyPilots();
            if (raw.isBlank()) { System.out.println("No pilots in your roster."); return true; }

            final List<String[]> pilots = new ArrayList<>();
            for (final String line : raw.split("\n")) pilots.add(line.split(";", 3));

            System.out.println();
            System.out.printf("%-4s %-35s %-10s %-30s%n", "No.", "EMAIL", "STATUS", "CERTIFIED MODELS");
            System.out.println("-".repeat(82));
            for (int i = 0; i < pilots.size(); i++) {
                final String[] f = pilots.get(i);
                System.out.printf("%-4d %-35s %-10s %-30s%n",
                        i + 1, f[0], f[1], f.length > 2 && !f[2].isBlank() ? f[2] : "—");
            }
            System.out.println();

            final String raw2 = Console.readLine("Select pilot number to remove (0 to cancel)");
            if (raw2 == null) return true;
            final int choice;
            try { choice = Integer.parseInt(raw2.trim()); }
            catch (final NumberFormatException e) { System.out.println("Invalid input."); return true; }

            if (choice == 0) { System.out.println("Cancelled."); return true; }
            if (choice < 1 || choice > pilots.size()) { System.out.println("Invalid selection."); return true; }

            final String userId = pilots.get(choice - 1)[0];
            final String confirm = Console.readLine("Remove pilot " + userId + " from your company? (Y/N)");
            if (!"Y".equalsIgnoreCase(confirm == null ? "" : confirm.trim())) {
                System.out.println("Cancelled.");
                return true;
            }
            System.out.println(AirTransportContext.service().removePilotFromCompany(userId));
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "Remove Pilot from Company"; }
}
