package airtransport.presentation;

import airtransport.presentation.flightplan.CreateFlightPlanAction;
import airtransport.presentation.flightroute.FlightRouteMenuAction;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractUI {

    private static final int EXIT_OPTION         = 0;
    private static final int FLEET_OPTION        = 1;
    private static final int FLIGHT_PLAN_OPTION  = 2;
    private static final int FLIGHT_ROUTE_OPTION = 3;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu();
        menu.addItem(FLEET_OPTION, "Fleet Management", new FleetMenuAction());
        menu.addItem(FLIGHT_PLAN_OPTION, "Create Flight Plan", new CreateFlightPlanAction());
        menu.addItem(FLIGHT_ROUTE_OPTION, "Flight Route Management", new FlightRouteMenuAction());
        menu.addItem(EXIT_OPTION, "Exit", () -> true);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Air Transport Console";
    }
}
