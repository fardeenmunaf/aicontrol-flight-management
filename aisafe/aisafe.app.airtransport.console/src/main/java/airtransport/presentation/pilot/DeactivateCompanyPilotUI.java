package airtransport.presentation.pilot;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class DeactivateCompanyPilotUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listCompanyPilots();
            if (raw.isBlank()) { System.out.println("No pilots in your roster."); return true; }

            // pilots CSV: userId;status;models — filter ACTIVE
            final List<String[]> active = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", 3);
                if ("ACTIVE".equals(f[1])) active.add(f);
            }

            if (active.isEmpty()) {
                System.out.println("No active pilots in your roster to deactivate.");
                return true;
            }

            System.out.println();
            System.out.printf("%-4s %-35s %-30s%n", "No.", "EMAIL", "CERTIFIED MODELS");
            System.out.println("-".repeat(72));
            for (int i = 0; i < active.size(); i++) {
                final String[] f = active.get(i);
                System.out.printf("%-4d %-35s %-30s%n",
                        i + 1, f[0], f.length > 2 && !f[2].isBlank() ? f[2] : "—");
            }
            System.out.println();

            final String raw2 = Console.readLine("Select pilot number to deactivate (0 to cancel)");
            if (raw2 == null) return true;
            final int choice;
            try { choice = Integer.parseInt(raw2.trim()); }
            catch (final NumberFormatException e) { System.out.println("Invalid input."); return true; }

            if (choice == 0) { System.out.println("Cancelled."); return true; }
            if (choice < 1 || choice > active.size()) { System.out.println("Invalid selection."); return true; }

            final String userId = active.get(choice - 1)[0];
            final String confirm = Console.readLine("Deactivate pilot " + userId + "? (Y/N)");
            if (!"Y".equalsIgnoreCase(confirm == null ? "" : confirm.trim())) {
                System.out.println("Cancelled.");
                return true;
            }
            System.out.println(AirTransportContext.service().deactivatePilot(userId));
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "Deactivate Pilot from Roster"; }
}
