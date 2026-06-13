package airtransport.presentation.flightplan;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class AddWeatherDataUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listFlightPlans();
            if (raw.isBlank()) {
                System.out.println("  No flight plans found for your company.");
                return false;
            }

            // plans CSV: id;designator;status;origin;dest;departure
            final List<String[]> editable = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                if ("DRAFT".equals(f[2]) || "TESTED".equals(f[2])) editable.add(f);
            }

            if (editable.isEmpty()) {
                System.out.println("  You have no editable flight plans (DRAFT or TESTED).");
                return false;
            }

            System.out.println();
            System.out.println("  Editable flight plans:");
            for (int i = 0; i < editable.size(); i++) {
                final String[] f = editable.get(i);
                System.out.printf("  %d. %s  [%s -> %s, status: %s]%n", i + 1, f[1], f[3], f[4], f[2]);
            }

            final String planInput = Console.readLine("  Select flight plan number (0 to cancel)");
            if (planInput == null || "0".equals(planInput.trim())) {
                System.out.println("  Cancelled.");
                return false;
            }

            final int planChoice;
            try { planChoice = Integer.parseInt(planInput.trim()); }
            catch (final NumberFormatException e) { System.out.println("  Invalid selection."); return false; }

            if (planChoice < 1 || planChoice > editable.size()) {
                System.out.println("  Invalid selection.");
                return false;
            }

            final String planId = editable.get(planChoice - 1)[0];
            System.out.println("  Searching for relevant weather data...");
            System.out.println();
            System.out.println(AirTransportContext.service().addWeatherData(planId));
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() { return "Add Weather Data to Flight Plan"; }
}
