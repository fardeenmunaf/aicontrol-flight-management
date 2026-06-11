package airtransport.presentation.flightplan;

import eapli.framework.presentation.console.AbstractUI;
import flightPlan.application.AddWeatherDataToFlightPlanController;
import flightPlan.application.AddWeatherResult;
import flightPlan.domain.FlightPlan;
import infrastructure.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

public class AddWeatherDataUI extends AbstractUI {

    private final AddWeatherDataToFlightPlanController controller = new AddWeatherDataToFlightPlanController(
            PersistenceContext.repositories().flightPlans(),
            PersistenceContext.repositories().weatherData(),
            PersistenceContext.repositories().airports(),
            PersistenceContext.repositories().nodes());

    @Override
    protected boolean doShow() {
        final List<FlightPlan> plans = new ArrayList<>();
        controller.editablePlansForPilot().forEach(plans::add);

        if (plans.isEmpty()) {
            System.out.println("  You have no editable flight plans (DRAFT or TESTED).");
            return false;
        }

        System.out.println();
        System.out.println("  Your editable flight plans:");
        for (int i = 0; i < plans.size(); i++) {
            final FlightPlan p = plans.get(i);
            System.out.printf("  %d. %s  [%s -> %s, status: %s]%n",
                    i + 1, p.flightDesignator(), p.routeOrigin(), p.routeDestination(), p.status());
        }

        final String planInput = eapli.framework.io.util.Console.readLine("  Select flight plan number (0 to cancel)");
        if (planInput == null || planInput.trim().equals("0")) {
            System.out.println("  Cancelled.");
            return false;
        }

        final int planChoice;
        try {
            planChoice = Integer.parseInt(planInput.trim());
        } catch (final NumberFormatException e) {
            System.out.println("  Invalid selection.");
            return false;
        }

        if (planChoice < 1 || planChoice > plans.size()) {
            System.out.println("  Invalid selection.");
            return false;
        }

        final FlightPlan selected = plans.get(planChoice - 1);

        try {
            System.out.println("  Searching for relevant weather data (temporal + spatial + altitude)...");
            final AddWeatherResult result = controller.addAllRelevantWeatherData(selected);
            System.out.println();
            if (result.addedCount() == 0) {
                System.out.println("  No weather data found that matches the time, position and altitude of this flight plan.");
            } else {
                if (result.testWasVoided()) {
                    System.out.println("  WARNING: The previous test result has been voided.");
                    System.out.println("           The flight plan must be re-validated before it can be used.");
                }
                System.out.printf("  %d weather record(s) successfully associated with flight plan %s.%n",
                        result.addedCount(), selected.flightDesignator());
            }
        } catch (final Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Weather Data to Flight Plan";
    }
}
