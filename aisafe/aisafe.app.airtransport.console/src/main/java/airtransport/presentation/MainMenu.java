package airtransport.presentation;

import airtransport.presentation.fleet.FleetMenuAction;
import airtransport.presentation.flightplan.AddWeatherDataAction;
import airtransport.presentation.flightplan.CreateFlightPlanAction;
import airtransport.presentation.flightplan.ValidateFlightPlanAction;
import airtransport.presentation.flightroute.FlightRouteMenuAction;
import airtransport.presentation.pilot.PilotManagementMenuAction;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractUI {

    private static final int EXIT_OPTION              = 0;
    private static final int FLEET_OPTION             = 1;
    private static final int FLIGHT_PLAN_OPTION       = 2;
    private static final int VALIDATE_PLAN_OPTION     = 3;
    private static final int FLIGHT_ROUTE_OPTION      = 4;
    private static final int PILOT_MGMT_OPTION        = 5;
    private static final int ADD_WEATHER_OPTION       = 6;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu();
        menu.addItem(FLEET_OPTION, "Fleet Management", new FleetMenuAction());
        menu.addItem(FLIGHT_PLAN_OPTION, "Create Flight Plan", new CreateFlightPlanAction());
        menu.addItem(VALIDATE_PLAN_OPTION, "Test/Validate Flight Plan", new ValidateFlightPlanAction());
        menu.addItem(FLIGHT_ROUTE_OPTION, "Flight Route Management", new FlightRouteMenuAction());
        menu.addItem(PILOT_MGMT_OPTION, "Pilot Management", new PilotManagementMenuAction());
        menu.addItem(ADD_WEATHER_OPTION, "Add Weather Data to Flight Plan", new AddWeatherDataAction());
        menu.addItem(EXIT_OPTION, "Exit", () -> true);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Air Transport Console";
    }
}
