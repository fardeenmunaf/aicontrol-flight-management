package backoffice.presentation.airtransportcompany;

import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class AirTransportCompaniesMenuUI extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_COMPANY_OPTION = 1;
    private static final int LIST_COMPANIES_OPTION = 2;
    private static final int LIST_FLEET_OPTION = 3;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = buildAirTransportCompaniesMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildAirTransportCompaniesMenu() {
        final Menu menu = new Menu("Air Transport Companies >");

        menu.addItem(REGISTER_COMPANY_OPTION, "Register New Company", new RegisterAirTransportCompanyAction());
        menu.addItem(LIST_COMPANIES_OPTION, "List All Companies", new ListAirTransportCompaniesAction());
        menu.addItem(LIST_FLEET_OPTION, "List Company Fleet", new ListFleetATCMenuAction());
        menu.addItem(RETURN_OPTION, "Return", Actions.SUCCESS);

        return menu;
    }

    @Override
    public String headline() {
        return "Air Transport Companies Management";
    }
}

