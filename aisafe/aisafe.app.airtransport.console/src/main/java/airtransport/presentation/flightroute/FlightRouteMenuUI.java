package airtransport.presentation.flightroute;

import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class FlightRouteMenuUI extends AbstractUI {

    private static final int CREATE_ROUTE  = 1;
    private static final int DELETE_ROUTE  = 2;
    private static final int RETURN        = 0;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("Flight Routes");
        menu.addItem(CREATE_ROUTE, "Create Flight Route (US073)", new CreateFlightRouteAction());
        menu.addItem(DELETE_ROUTE, "Deactivate Flight Route (US074)", new DeleteFlightRouteAction());
        menu.addItem(RETURN, "Return", () -> false);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Flight Route Management";
    }
}
