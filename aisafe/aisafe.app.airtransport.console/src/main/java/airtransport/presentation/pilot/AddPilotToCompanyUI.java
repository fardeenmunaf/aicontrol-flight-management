package airtransport.presentation.pilot;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class AddPilotToCompanyUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listAvailablePilots();
            if (raw.isBlank()) {
                System.out.println("No unassigned pilots available.");
                return true;
            }

            // pilots CSV: userId;status;models
            final List<String[]> available = new ArrayList<>();
            for (final String line : raw.split("\n")) available.add(line.split(";", 3));

            System.out.println();
            System.out.printf("%-4s %-35s %-10s %-30s%n", "No.", "EMAIL", "STATUS", "CERTIFIED MODELS");
            System.out.println("-".repeat(82));
            for (int i = 0; i < available.size(); i++) {
                final String[] f = available.get(i);
                System.out.printf("%-4d %-35s %-10s %-30s%n",
                        i + 1, f[0], f[1], f.length > 2 && !f[2].isBlank() ? f[2] : "—");
            }
            System.out.println();

            final String raw2 = Console.readLine("Select pilot number to add (0 to cancel)");
            if (raw2 == null) return true;
            final int choice;
            try { choice = Integer.parseInt(raw2.trim()); }
            catch (final NumberFormatException e) { System.out.println("Invalid input."); return true; }

            if (choice == 0) { System.out.println("Cancelled."); return true; }
            if (choice < 1 || choice > available.size()) { System.out.println("Invalid selection."); return true; }

            System.out.println(AirTransportContext.service().addPilotToCompany(available.get(choice - 1)[0]));
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "Add Pilot to My Company"; }
}
