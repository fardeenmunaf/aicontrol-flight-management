package backoffice.presentation.airport;

import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class AirportsMenu extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_AIRPORT_OPTION = 1;
    private static final int LIST_AIRPORTS_OPTION = 2;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("Airports");

        menu.addItem(REGISTER_AIRPORT_OPTION, "Register Airport",
                new RegisterAirportAction());
        menu.addItem(LIST_AIRPORTS_OPTION, "List Airports",
                new ListAirportsAction());

        menu.addItem(RETURN_OPTION, "Return", () -> true);

        final MenuRenderer renderer =
                new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Airports";
    }
}