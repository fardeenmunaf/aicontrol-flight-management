package airtransport.presentation.flightplan;

import airtransport.AirTransportContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class ValidateFlightPlanUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listFlightPlans();
            if (raw.isBlank()) {
                System.out.println("  No flight plans found for your company.");
                return false;
            }

            // plans CSV: id;designator;status;origin;dest;departure
            final List<String[]> drafts = new ArrayList<>();
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                if ("DRAFT".equals(f[2])) drafts.add(f);
            }

            if (drafts.isEmpty()) {
                System.out.println("  No flight plans in DRAFT status.");
                return false;
            }

            System.out.println();
            System.out.println("  DRAFT flight plans:");
            for (int i = 0; i < drafts.size(); i++) {
                final String[] f = drafts.get(i);
                System.out.printf("  %d. %s  [%s -> %s, departure: %s]%n",
                        i + 1, f[1], f[3], f[4], f[5]);
            }

            final String input = Console.readLine("  Select flight plan number (0 to cancel)");
            if (input == null || "0".equals(input.trim())) {
                System.out.println("  Cancelled.");
                return false;
            }

            final int choice;
            try { choice = Integer.parseInt(input.trim()); }
            catch (final NumberFormatException e) { System.out.println("  Invalid selection."); return false; }

            if (choice < 1 || choice > drafts.size()) {
                System.out.println("  Invalid selection.");
                return false;
            }

            final String planId = drafts.get(choice - 1)[0];
            System.out.println("  Running validation for: " + drafts.get(choice - 1)[1] + " ...");
            System.out.println();
            System.out.println(AirTransportContext.service().validateFlightPlan(planId));
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() { return "Test/Validate Flight Plan"; }
}
