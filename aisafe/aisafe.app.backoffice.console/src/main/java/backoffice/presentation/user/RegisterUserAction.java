package backoffice.presentation.user;

import eapli.framework.actions.Action;

public class RegisterUserAction implements Action {

    @Override
    public boolean execute() {
        new RegisterUserUI().show();
        return false;
    }
}

