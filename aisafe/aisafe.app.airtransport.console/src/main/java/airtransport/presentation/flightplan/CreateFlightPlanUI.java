package airtransport.presentation.flightplan;

import airtransport.AirTransportContext;
import airtransport.service.LocalAirTransportService;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateFlightPlanUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        System.out.println();
        System.out.println("  1. Import from a DSL file");
        System.out.println("  2. Paste DSL text");
        final boolean localMode = AirTransportContext.service() instanceof LocalAirTransportService;
        if (localMode) System.out.println("  3. Interactive builder");
        final String choice = Console.readLine("  Select option");
        final String trimmed = choice == null ? "" : choice.trim();

        if ("1".equals(trimmed)) {
            importFromFile();
        } else if ("2".equals(trimmed)) {
            pasteText();
        } else if ("3".equals(trimmed) && localMode) {
            createInteractively();
        } else {
            System.out.println("  Cancelled.");
        }
        return false;
    }

    private void importFromFile() {
        final String path = promptNonEmpty("Path to DSL file");
        try {
            final String dslText = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            System.out.println(AirTransportContext.service().createFlightPlan(dslText));
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    private void pasteText() {
        System.out.println("  Paste DSL text (end with a line containing only '.'):");
        final StringBuilder sb = new StringBuilder();
        while (true) {
            final String line = Console.readLine("");
            if (line == null || ".".equals(line.trim())) break;
            sb.append(line).append("\n");
        }
        if (sb.toString().isBlank()) {
            System.out.println("  No text entered.");
            return;
        }
        System.out.println(AirTransportContext.service().createFlightPlan(sb.toString()));
    }

    private void createInteractively() {
        final LocalAirTransportService local = (LocalAirTransportService) AirTransportContext.service();
        System.out.println("  Profile/segment detail");
        System.out.println("  1. template");
        System.out.println("  2. prompt everything");
        final String detail = Console.readLine("  Select option [1]");
        final boolean promptEverything = "2".equals(detail == null ? "" : detail.trim());

        final String pilotEmail = promptNonEmpty("Pilot email (will appear in flight plan)");

        try {
            final String dsl = new ConsoleFlightDslBuilder(
                    pilotEmail, promptEverything,
                    local.getCompanyAircraft(),
                    local.getCompanyRoutes(),
                    local.getCompanyIcaoCode()).build();
            System.out.println(AirTransportContext.service().createFlightPlan(dsl));
        } catch (final IllegalArgumentException e) {
            System.out.println("  Validation error: " + e.getMessage());
        }
    }

    private static String promptNonEmpty(final String label) {
        while (true) {
            final String raw = Console.readLine("  " + label);
            if (raw != null && !raw.trim().isEmpty()) return raw.trim();
            System.out.println("  This field is required.");
        }
    }

    @Override
    public String headline() { return "Create Flight Plan"; }
}
