package airtransport.presentation.fleet;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class ListFleetByCapacityUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String input = Console.readLine("Enter minimum total passenger capacity");
            if (input == null || input.isBlank()) {
                System.out.println("Capacity cannot be empty.");
                return true;
            }
            final int minCapacity;
            try {
                minCapacity = Integer.parseInt(input.trim());
            } catch (final NumberFormatException e) {
                System.out.println("Invalid number: " + input.trim());
                return true;
            }
            if (minCapacity < 0) {
                System.out.println("Capacity cannot be negative.");
                return true;
            }

            final String raw = AirTransportContext.service().listFleet();
            if (raw.isBlank()) {
                System.out.println("No aircraft in your fleet.");
                return true;
            }

            // fleet CSV: reg;model;maker;status;econ;bus;first;country;aircraftId
            final List<String[]> filtered = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                final int total = Integer.parseInt(f[4]) + Integer.parseInt(f[5]) + Integer.parseInt(f[6]);
                if (total >= minCapacity) filtered.add(f);
            }

            System.out.printf("%n=== Fleet — Capacity >= %d ===%n%n", minCapacity);
            if (filtered.isEmpty()) {
                System.out.println("No aircraft found with capacity >= " + minCapacity + ".");
                return true;
            }

            System.out.printf("%-12s | %-25s | %-10s | %-10s | %-10s | %-12s | %-12s%n",
                    "REGISTRATION", "MODEL", "ECONOMY", "BUSINESS", "FIRST", "TOTAL", "STATUS");
            System.out.println("-".repeat(110));
            for (final String[] f : filtered) {
                final int eco = Integer.parseInt(f[4]), bus = Integer.parseInt(f[5]), first = Integer.parseInt(f[6]);
                System.out.printf("%-12s | %-25s | %-10d | %-10d | %-10d | %-12d | %-12s%n",
                        f[0], f[1], eco, bus, first, eco + bus + first, f[3]);
            }
            System.out.println("-".repeat(110));
            System.out.printf("Total Aircraft: %d%n", filtered.size());
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "List Fleet By Capacity"; }
}
