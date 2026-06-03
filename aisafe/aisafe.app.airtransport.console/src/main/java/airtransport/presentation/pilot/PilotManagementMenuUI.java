package airtransport.presentation.pilot;

import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class PilotManagementMenuUI extends AbstractUI {

    private static final int RETURN_OPTION           = 0;
    private static final int ADD_PILOT_OPTION        = 1;
    private static final int LIST_PILOTS_OPTION      = 2;
    private static final int DEACTIVATE_PILOT_OPTION = 3;
    private static final int ACTIVATE_PILOT_OPTION   = 4;
    private static final int REMOVE_PILOT_OPTION     = 5;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("Pilot Management >");
        menu.addItem(ADD_PILOT_OPTION, "Add Pilot to My Company", new AddPilotToCompanyAction());
        menu.addItem(LIST_PILOTS_OPTION, "List My Company's Pilots", new ListCompanyPilotsAction());
        menu.addItem(DEACTIVATE_PILOT_OPTION, "Deactivate Pilot", new DeactivateCompanyPilotAction());
        menu.addItem(ACTIVATE_PILOT_OPTION, "Activate Pilot", new ActivateCompanyPilotAction());
        menu.addItem(REMOVE_PILOT_OPTION, "Remove Pilot from Company", new RemovePilotFromCompanyAction());
        menu.addItem(RETURN_OPTION, "Return", Actions.SUCCESS);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Pilot Management";
    }
}
