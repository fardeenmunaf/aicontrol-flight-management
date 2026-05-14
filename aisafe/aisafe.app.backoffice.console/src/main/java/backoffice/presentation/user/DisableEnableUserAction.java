package backoffice.presentation.user;

import eapli.framework.actions.Action;

public class DisableEnableUserAction implements Action {

    @Override
    public boolean execute() {
        new DisableEnableUserUI().show();
        return false;
    }
}

