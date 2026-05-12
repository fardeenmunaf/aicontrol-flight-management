package backoffice.presentation.aircontrolarea;

import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class AirControlAreasMenuUI extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_AREA_OPTION = 1;
    private static final int LIST_AREAS_OPTION = 2;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = buildAirControlAreasMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildAirControlAreasMenu() {
        final Menu menu = new Menu("Air Control Areas >");

        menu.addItem(REGISTER_AREA_OPTION, "Register New Air Control Area", new RegisterAirControlAreaAction());
        menu.addItem(LIST_AREAS_OPTION, "List All Air Control Areas", new ListAirControlAreasAction());
        menu.addItem(RETURN_OPTION, "Return", Actions.SUCCESS);

        return menu;
    }

    @Override
    public String headline() {
        return "Air Control Areas Management";
    }
}