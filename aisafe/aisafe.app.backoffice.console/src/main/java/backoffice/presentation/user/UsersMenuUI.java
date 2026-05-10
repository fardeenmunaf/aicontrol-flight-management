package backoffice.presentation.user;

import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class UsersMenuUI extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_USER_OPTION = 1;
    private static final int LIST_USERS_OPTION = 2;
    private static final int DISABLE_USER_OPTION = 3;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = buildUsersMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildUsersMenu() {
        final Menu menu = new Menu("Users >");

        menu.addItem(REGISTER_USER_OPTION, "Register New User", new RegisterUserAction());
        menu.addItem(LIST_USERS_OPTION, "List All Users", new ListUsersAction());
        menu.addItem(DISABLE_USER_OPTION, "Disable/Enable User", new DisableEnableUserAction());
        menu.addItem(RETURN_OPTION, "Return", Actions.SUCCESS);

        return menu;
    }

    @Override
    public String headline() {
        return "Users Management";
    }
}

