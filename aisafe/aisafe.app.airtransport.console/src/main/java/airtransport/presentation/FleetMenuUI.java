package airtransport.presentation;

import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class FleetMenuUI extends AbstractUI {

    private static final int REGISTER_AIRCRAFT = 1;
    private static final int LIST_AIRCRAFT = 2;
    private static final int DECOMMISSION_AIRCRAFT = 3;
    private static final int RETURN = 0;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("Fleet Management");
        menu.addItem(REGISTER_AIRCRAFT, "Register Aircraft", new RegisterAircraftAction());
        menu.addItem(LIST_AIRCRAFT, "List Aircraft", new ListAircraftAction());
        menu.addItem(DECOMMISSION_AIRCRAFT, "Decommission Aircraft", new DecommissionAircraftAction());
        menu.addItem(RETURN, "Return", () -> false);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Fleet Management";
    }
}
