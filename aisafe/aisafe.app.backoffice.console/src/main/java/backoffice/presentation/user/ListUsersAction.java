package backoffice.presentation.user;

import eapli.framework.actions.Action;

public class ListUsersAction implements Action {

    @Override
    public boolean execute() {
        new ListUsersUI().show();
        return false;
    }
}

