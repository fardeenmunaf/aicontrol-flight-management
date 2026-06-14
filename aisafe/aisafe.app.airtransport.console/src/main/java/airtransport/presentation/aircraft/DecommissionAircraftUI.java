package airtransport.presentation.aircraft;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class DecommissionAircraftUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listFleet();
            if (raw.isBlank()) {
                System.out.println("No aircraft in your fleet.");
                return true;
            }

            // fleet CSV: reg;model;maker;status;econ;bus;first;country;aircraftId
            final List<String[]> active = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                if (!"DECOMISSIONED".equals(f[3])) active.add(f);
            }

            if (active.isEmpty()) {
                System.out.println("No active aircraft available to decommission.");
                return true;
            }

            System.out.println("Active aircraft in your fleet:");
            for (int i = 0; i < active.size(); i++) {
                final String[] f = active.get(i);
                System.out.printf("%d) %s | %s | %s | Status: %s%n",
                        i + 1, f[0], f[7], f[1], f[3]);
            }

            final int choice = readInt("Select aircraft number to decommission");
            if (choice < 1 || choice > active.size()) {
                System.out.println("Invalid selection.");
                return true;
            }

            final String[] selected = active.get(choice - 1);
            System.out.printf("%nAircraft: %s (%s) — Status: %s%n", selected[0], selected[7], selected[3]);

            final String confirm = Console.readLine("Decommission this aircraft? (yes/no)");
            if (confirm == null || !confirm.trim().equalsIgnoreCase("yes")) {
                System.out.println("Cancelled.");
                return true;
            }

            // f[8] = aircraftId
            System.out.println(AirTransportContext.service().decommissionAircraft(selected[8]));
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "Decommission Aircraft"; }

    private int readInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) throw new IllegalArgumentException("No input provided");
        try { return Integer.parseInt(raw.trim()); }
        catch (final NumberFormatException e) { throw new IllegalArgumentException("Invalid number: " + raw); }
    }
}
