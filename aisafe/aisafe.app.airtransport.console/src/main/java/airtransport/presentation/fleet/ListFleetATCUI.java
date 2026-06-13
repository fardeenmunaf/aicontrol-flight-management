package airtransport.presentation.fleet;

import airtransport.AirTransportContext;
import eapli.framework.presentation.console.AbstractUI;

public class ListFleetATCUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listFleet();
            if (raw.isBlank()) {
                System.out.println("No aircraft registered for your company.");
                return true;
            }
            System.out.println();
            // fleet CSV: reg;model;maker;status;econ;bus;first;country;aircraftId
            System.out.printf("%-12s | %-25s | %-12s | %-30s | %-10s%n",
                    "REGISTRATION", "MODEL", "STATUS", "CABIN (eco/bus/first)", "COUNTRY");
            System.out.println("-".repeat(100));
            final String[] lines = raw.split("\n");
            for (final String line : lines) {
                final String[] f = line.split(";", -1);
                final String cabin = "eco:" + f[4] + " bus:" + f[5] + " first:" + f[6];
                System.out.printf("%-12s | %-25s | %-12s | %-30s | %-10s%n",
                        f[0], f[1], f[3], cabin, f[7]);
            }
            System.out.println("-".repeat(100));
            System.out.printf("Total Aircraft: %d%n", lines.length);
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "List Fleet"; }
}
