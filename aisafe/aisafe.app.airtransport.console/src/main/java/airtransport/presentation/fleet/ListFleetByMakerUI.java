package airtransport.presentation.fleet;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class ListFleetByMakerUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String makerFilter = Console.readLine("Enter maker name to search");
            if (makerFilter == null || makerFilter.isBlank()) {
                System.out.println("Maker name cannot be empty.");
                return true;
            }
            final String lower = makerFilter.trim().toLowerCase();

            final String raw = AirTransportContext.service().listFleet();
            if (raw.isBlank()) { System.out.println("No aircraft in your fleet."); return true; }

            // fleet CSV: reg;model;maker;status;econ;bus;first;country;aircraftId
            final List<String[]> filtered = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                if (f[2].toLowerCase().contains(lower)) filtered.add(f);
            }

            System.out.printf("%n=== Fleet — Maker: \"%s\" ===%n%n", makerFilter.trim());
            if (filtered.isEmpty()) {
                System.out.println("No aircraft found with maker \"" + makerFilter.trim() + "\".");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-15s | %-12s | %-10s%n",
                    "REGISTRATION", "MODEL", "MAKER", "STATUS", "COUNTRY");
            System.out.println("-".repeat(90));
            for (final String[] f : filtered) {
                System.out.printf("%-12s | %-25s | %-15s | %-12s | %-10s%n",
                        f[0], f[1], f[2], f[3], f[7]);
            }
            System.out.println("-".repeat(90));
            System.out.printf("Total Aircraft: %d%n", filtered.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "List Fleet By Maker"; }
}
