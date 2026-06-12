package pilot.presentation.flightplan;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CreateFlightPlanRemoteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        System.out.println();
        System.out.println("  1. Import from DSL file");
        System.out.println("  2. Create interactively");
        System.out.println("  3. Paste DSL text");
        final String choice = Console.readLine("  Select option");
        final String trimmed = choice == null ? "" : choice.trim();
        try {
            switch (trimmed) {
                case "1" -> importFromFile();
                case "2" -> createInteractively();
                case "3" -> pasteDsl();
                default  -> System.out.println("  Cancelled.");
            }
        } catch (final IOException e) {
            System.out.println("  Error communicating with server: " + e.getMessage());
        }
        return false;
    }

    private void importFromFile() throws IOException {
        final String path = promptNonEmpty("Path to DSL file");
        final String dsl;
        try {
            dsl = Files.readString(Paths.get(path));
        } catch (final IOException e) {
            System.out.println("  Cannot read file: " + e.getMessage());
            return;
        }
        sendAndReport(dsl);
    }

    private void createInteractively() throws IOException {
        System.out.println("\n  Fetching fleet and routes from server...");
        final String fleetResponse  = PilotClientSession.getClient().listFleet();
        final String routesResponse = PilotClientSession.getClient().listRoutes();

        final List<String[]> fleet  = parseRows(fleetResponse);
        final List<String[]> routes = parseRows(routesResponse);

        if (fleet.isEmpty()) {
            System.out.println("  No certified aircraft found in your company fleet.");
            return;
        }
        if (routes.isEmpty()) {
            System.out.println("  No routes found for your company.");
            return;
        }

        System.out.println("\n  Profile/segment detail:");
        System.out.println("  1. Template (recommended defaults)");
        System.out.println("  2. Prompt everything");
        final String detail = Console.readLine("  Select option [1]");
        final boolean promptEverything = "2".equals(detail == null ? "" : detail.trim());

        final String pilotEmail = PilotClientSession.getPilotEmail();

        try {
            final String dsl = new RemoteFlightDslBuilder(pilotEmail, promptEverything, fleet, routes).build();
            System.out.println("\n  --- Generated DSL ---");
            System.out.println(dsl);
            System.out.println("  --- End DSL ---\n");
            sendAndReport(dsl);
        } catch (final IllegalArgumentException e) {
            System.out.println("  Validation error: " + e.getMessage());
        }
    }

    private void pasteDsl() throws IOException {
        System.out.println("  Paste DSL text. Enter a single '.' on a blank line when done.");
        final StringBuilder sb = new StringBuilder();
        while (true) {
            final String line = Console.readLine("");
            if (".".equals(line == null ? "" : line.trim())) break;
            if (line != null) sb.append(line).append("\n");
        }
        final String dsl = sb.toString().trim();
        if (dsl.isEmpty()) {
            System.out.println("  Empty input, cancelled.");
            return;
        }
        sendAndReport(dsl);
    }

    private void sendAndReport(final String dsl) throws IOException {
        System.out.println("  Sending to server...");
        final String response = PilotClientSession.getClient().createPlan(dsl);
        System.out.println("\n  " + response.replace("\n", "\n  "));
    }

    private static List<String[]> parseRows(final String response) {
        final List<String[]> rows = new ArrayList<>();
        if (response == null || response.isBlank()) return rows;
        for (final String line : response.split("\n")) {
            if (!line.isBlank()) rows.add(line.split(";", -1));
        }
        return rows;
    }

    private static String promptNonEmpty(final String label) {
        while (true) {
            final String raw = Console.readLine("  " + label);
            if (raw != null && !raw.trim().isEmpty()) return raw.trim();
            System.out.println("  Required.");
        }
    }

    @Override
    public String headline() {
        return "Create Flight Plan (Remote)";
    }
}
