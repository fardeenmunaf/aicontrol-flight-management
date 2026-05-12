package backoffice.presentation.customercollaborator;

import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class CustomerCollaboratorsMenu extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_OPTION = 1;
    private static final int LIST_OPTION = 2;
    private static final int DISABLE_OPTION = 3;
    private static final int UPDATE_OPTION = 4;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("Customer Collaborators");
        menu.addItem(REGISTER_OPTION, "Register customer collaborator", new RegisterCustomerCollaboratorAction());
        menu.addItem(LIST_OPTION, "List customer collaborators", new ListCustomerCollaboratorsAction());
        menu.addItem(UPDATE_OPTION, "Update customer collaborator contact information", new UpdateCustomerCollaboratorAction());
        menu.addItem(DISABLE_OPTION, "Disable customer collaborator", new DisableCustomerCollaboratorAction());
        menu.addItem(RETURN_OPTION, "Return", () -> true);

        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Customer Collaborators";
    }
}