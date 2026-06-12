package pilot.presentation.flightplan;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import pilot.PilotClientSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateFlightPlanRemoteUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        System.out.println("\n  1. Import from a DSL file");
        System.out.println("  2. Paste DSL text directly");
        final String choice = Console.readLine("  Select option");

        final String dslText;
        if ("1".equals(choice == null ? "" : choice.trim())) {
            final String path = Console.readLine("  Path to DSL file");
            if (path == null || path.isBlank()) {
                System.out.println("  Cancelled.");
                return false;
            }
            try {
                dslText = Files.readString(Paths.get(path.trim()));
            } catch (final IOException e) {
                System.out.println("  Could not read file: " + e.getMessage());
                return false;
            }
        } else if ("2".equals(choice == null ? "" : choice.trim())) {
            System.out.println("  Paste DSL text (enter a single '.' on a blank line to finish):");
            final StringBuilder sb = new StringBuilder();
            while (true) {
                final String line = Console.readLine("");
                if (".".equals(line)) break;
                sb.append(line).append("\n");
            }
            dslText = sb.toString();
        } else {
            System.out.println("  Cancelled.");
            return false;
        }

        if (dslText.isBlank()) {
            System.out.println("  DSL text is empty. Cancelled.");
            return false;
        }

        try {
            System.out.println("  Sending DSL to server...");
            final String response = PilotClientSession.getClient().createPlan(dslText);
            System.out.println("\n  Server response:");
            System.out.println("  " + response.replace("\n", "\n  "));
        } catch (final IOException e) {
            System.out.println("  Error communicating with server: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Create Flight Plan (Remote)";
    }
}
