package airtransport.presentation.flightplan;

import eapli.framework.presentation.console.AbstractUI;
import flight.dsl.mapping.ValidateFlightPlanService;
import flightPlan.application.FlightPlanValidationResult;
import flightPlan.application.ValidateFlightPlanController;
import flightPlan.domain.FlightPlan;
import infrastructure.persistence.PersistenceContext;
import shared.domain.FlightPlanId;

import java.util.ArrayList;
import java.util.List;

public class ValidateFlightPlanUI extends AbstractUI {

    private final ValidateFlightPlanService service = new ValidateFlightPlanService(
            new ValidateFlightPlanController(PersistenceContext.repositories().flightPlans()),
            PersistenceContext.repositories().airports());

    @Override
    protected boolean doShow() {
        final List<FlightPlan> drafts = new ArrayList<>();
        service.draftPlansForPilot().forEach(drafts::add);

        if (drafts.isEmpty()) {
            System.out.println("  You have no flight plans in DRAFT status.");
            return false;
        }

        System.out.println();
        System.out.println("  Your DRAFT flight plans:");
        for (int i = 0; i < drafts.size(); i++) {
            final FlightPlan p = drafts.get(i);
            System.out.printf("  %d. %s  [%s → %s, departure: %s]%n",
                    i + 1,
                    p.flightDesignator(),
                    p.routeOrigin(),
                    p.routeDestination(),
                    p.departureTime());
        }

        final String input = eapli.framework.io.util.Console.readLine("  Select flight plan number (0 to cancel)");
        if (input == null || input.trim().equals("0")) {
            System.out.println("  Cancelled.");
            return false;
        }

        final int choice;
        try {
            choice = Integer.parseInt(input.trim());
        } catch (final NumberFormatException e) {
            System.out.println("  Invalid selection.");
            return false;
        }

        if (choice < 1 || choice > drafts.size()) {
            System.out.println("  Invalid selection.");
            return false;
        }

        final FlightPlan selected = drafts.get(choice - 1);
        System.out.println("  Running validation for: " + selected.flightDesignator() + " ...");

        final FlightPlanValidationResult result;
        try {
            result = service.validate(selected.identity());
        } catch (final SecurityException e) {
            System.out.println("  ERROR: " + e.getMessage());
            return false;
        } catch (final IllegalStateException e) {
            System.out.println("  ERROR: " + e.getMessage());
            return false;
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
            return false;
        }

        printResult(result, selected.flightDesignator());
        return false;
    }

    private static void printResult(final FlightPlanValidationResult result, final String designator) {
        System.out.println();
        if (!result.dslErrors().isEmpty()) {
            System.out.println("  DSL Validation FAILED:");
            result.dslErrors().forEach(e -> System.out.println("    [DSL] " + e));
        } else {
            System.out.println("  DSL Validation: PASSED");
        }

        if (result.physicalCheckRan()) {
            if (!result.physicalFailures().isEmpty()) {
                System.out.println("  Physical Validation FAILED:");
                result.physicalFailures().forEach(f -> System.out.println("    [PHYSICAL] " + f));
            } else {
                System.out.println("  Physical Validation: PASSED");
            }
        } else {
            System.out.println("  Physical Validation: NOT RUN (DSL errors must be fixed first)");
        }

        System.out.println();
        if (result.passed()) {
            System.out.println("  ✓ Flight plan " + designator + " has been TESTED successfully. Status updated to TESTED.");
        } else {
            System.out.println("  ✗ Flight plan " + designator + " validation FAILED. Status remains DRAFT.");
        }
    }

    @Override
    public String headline() {
        return "Test/Validate Flight Plan";
    }
}
