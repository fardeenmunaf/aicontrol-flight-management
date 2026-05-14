package backoffice.presentation.user;

import eapli.framework.actions.Action;

public class UsersMenuAction implements Action {

    @Override
    public boolean execute() {
        new UsersMenuUI().mainLoop();
        return false;
    }
}

