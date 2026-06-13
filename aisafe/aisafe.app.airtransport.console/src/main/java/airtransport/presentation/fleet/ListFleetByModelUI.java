package airtransport.presentation.fleet;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class ListFleetByModelUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String modelFilter = Console.readLine("Enter model name to search");
            if (modelFilter == null || modelFilter.isBlank()) {
                System.out.println("Model name cannot be empty.");
                return true;
            }
            final String lower = modelFilter.trim().toLowerCase();

            final String raw = AirTransportContext.service().listFleet();
            if (raw.isBlank()) { System.out.println("No aircraft in your fleet."); return true; }

            // fleet CSV: reg;model;maker;status;econ;bus;first;country;aircraftId
            final List<String[]> filtered = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                if (f[1].toLowerCase().contains(lower)) filtered.add(f);
            }

            System.out.printf("%n=== Fleet — Model: \"%s\" ===%n%n", modelFilter.trim());
            if (filtered.isEmpty()) {
                System.out.println("No aircraft found matching model \"" + modelFilter.trim() + "\".");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-12s | %-30s | %-10s%n",
                    "REGISTRATION", "MODEL", "STATUS", "CABIN (eco/bus/first)", "COUNTRY");
            System.out.println("-".repeat(100));
            for (final String[] f : filtered) {
                final String cabin = "eco:" + f[4] + " bus:" + f[5] + " first:" + f[6];
                System.out.printf("%-12s | %-25s | %-12s | %-30s | %-10s%n",
                        f[0], f[1], f[3], cabin, f[7]);
            }
            System.out.println("-".repeat(100));
            System.out.printf("Total Aircraft: %d%n", filtered.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "List Fleet By Model"; }
}
