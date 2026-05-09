package backoffice.presentation.aircraftmodel;

import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class AircraftModelsMenuUI extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_AIRCRAFT_MODEL_OPTION = 1;
    private static final int VIEW_AVAILABLE_AIRCRAFT_MODEL_OPTION = 2;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = buildAircraftModelsMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildAircraftModelsMenu() {
        final Menu menu = new Menu("Aircraft Models");

        menu.addItem(REGISTER_AIRCRAFT_MODEL_OPTION, "Register Aircraft Model", new RegisterAircraftModelAction());
        menu.addItem(VIEW_AVAILABLE_AIRCRAFT_MODEL_OPTION, "View Available Aircraft Models", new ViewAircraftModelAction());
        menu.addItem(RETURN_OPTION, "Return", Actions.SUCCESS);

        return menu;
    }

    @Override
    public String headline() {
        return "Aircraft Models Management";
    }
}

