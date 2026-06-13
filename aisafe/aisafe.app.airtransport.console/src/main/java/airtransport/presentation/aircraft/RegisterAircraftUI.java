package airtransport.presentation.aircraft;

import airtransport.AirTransportContext;
import aircraft.domain.AircraftOperationStatus;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class RegisterAircraftUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        try {
            final String raw = AirTransportContext.service().listAircraftModels();
            if (raw.isBlank()) {
                System.out.println("No aircraft models available. Create aircraft models first.");
                return true;
            }

            final String[] lines = raw.split("\n");
            System.out.println("Available aircraft models:");
            for (int i = 0; i < lines.length; i++) {
                final String[] f = lines[i].split(";", -1);
                // f: modelId;name;maker;engines;mzfw;emptyWeight;engId1:engName1|...
                System.out.printf("%d) %s (%s) - Engines: %s%n", i + 1, f[1], f[2], f[3]);
            }

            final int modelChoice = readInt("Select aircraft model number");
            if (modelChoice < 1 || modelChoice > lines.length) {
                System.out.println("Invalid selection.");
                return true;
            }

            final String[] mf = lines[modelChoice - 1].split(";", -1);
            final String modelId  = mf[0];
            final double mzfw     = Double.parseDouble(mf[4]);
            final double empty    = Double.parseDouble(mf[5]);
            final String engPart  = mf.length > 6 ? mf[6] : "";

            final List<String[]> engines = new ArrayList<>();
            if (!engPart.isBlank()) {
                for (final String pair : engPart.split("\\|")) {
                    if (!pair.isBlank()) engines.add(pair.split(":", 2));
                }
            }

            if (engines.isEmpty()) {
                System.out.println("No certified engines for this model. Contact admin.");
                return true;
            }

            System.out.println("Certified engines for " + mf[1] + ":");
            for (int i = 0; i < engines.size(); i++) {
                System.out.printf("%d) %s%n", i + 1, engines.get(i)[1]);
            }

            final int engChoice = readInt("Select installed engine number");
            if (engChoice < 1 || engChoice > engines.size()) {
                System.out.println("Invalid selection.");
                return true;
            }
            final String engineId = engines.get(engChoice - 1)[0];

            final double payloadBudget = mzfw - empty - 500.0;
            final int estimatedMaxPeople = (int) Math.floor(Math.max(0.0, payloadBudget) / 100.0);
            System.out.printf("Estimated max onboard people: %d%n", estimatedMaxPeople);

            final String reg     = readLine("Registration number");
            final String country = readLine("Registration country");
            final AircraftOperationStatus status = chooseOperationStatus();
            final int crew = readInt("Flight crew count");

            final int estimatedMaxSeats = Math.max(0, estimatedMaxPeople - crew);
            System.out.printf("Estimated max passenger seats with crew %d: %d%n", crew, estimatedMaxSeats);

            final int econ  = readInt("Economy seats");
            final int bus   = readInt("Business seats");
            final int first = readInt("First class seats");

            if ((econ + bus + first) > estimatedMaxSeats) {
                System.out.printf("Total seats (%d) exceed estimated limit (%d).%n", econ + bus + first, estimatedMaxSeats);
                return true;
            }

            final String result = AirTransportContext.service()
                    .registerAircraft(reg, country, status.name(), crew, modelId, engineId, econ, bus, first);
            System.out.println(result);
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() { return "Register Aircraft"; }

    private String readLine(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) throw new IllegalArgumentException("No input provided");
        return raw.trim();
    }

    private int readInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) throw new IllegalArgumentException("No input provided");
        try { return Integer.parseInt(raw.trim()); }
        catch (final NumberFormatException e) { throw new IllegalArgumentException("Invalid number: " + raw); }
    }

    private AircraftOperationStatus chooseOperationStatus() {
        final AircraftOperationStatus[] statuses = AircraftOperationStatus.values();
        System.out.println("Available operation statuses:");
        for (int i = 0; i < statuses.length; i++) {
            System.out.printf("%d) %s%n", i + 1, statuses[i].name());
        }
        final int choice = readInt("Select operation status number");
        if (choice < 1 || choice > statuses.length) throw new IllegalArgumentException("Invalid status selection");
        return statuses[choice - 1];
    }
}
