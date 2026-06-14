package airtransport.presentation.pilot;

import airtransport.AirTransportContext;
import eapli.framework.presentation.console.AbstractUI;

public class ListCompanyPilotsUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listCompanyPilots();
            if (raw.isBlank()) {
                System.out.println("No pilots assigned to your company.");
                return true;
            }
            System.out.println();
            // pilots CSV: userId;status;models
            System.out.printf("%-35s %-10s %-30s%n", "EMAIL", "STATUS", "CERTIFIED MODELS");
            System.out.println("-".repeat(78));
            int count = 0;
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", 3);
                System.out.printf("%-35s %-10s %-30s%n",
                        f[0], f[1], f.length > 2 && !f[2].isBlank() ? f[2] : "—");
                count++;
            }
            System.out.println();
            System.out.println(count + " pilot(s) in your roster.");
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "My Company's Pilot Roster"; }
}
