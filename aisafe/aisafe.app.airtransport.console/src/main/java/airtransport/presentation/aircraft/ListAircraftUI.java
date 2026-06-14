package airtransport.presentation.aircraft;

import airtransport.AirTransportContext;
import eapli.framework.presentation.console.AbstractUI;

public class ListAircraftUI extends AbstractUI {

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
            for (final String line : raw.split("\n")) {
                final String[] f = line.split(";", -1);
                System.out.printf("Registration: %-12s | Country: %-5s | Model: %-25s | Status: %s%n",
                        f[0], f[7], f[1], f[3]);
            }
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "List Aircraft"; }
}
