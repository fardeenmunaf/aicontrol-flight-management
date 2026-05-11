package backoffice.presentation.customercollaborator;

import eapli.framework.actions.Action;

public class RegisterCustomerCollaboratorAction implements Action {
    @Override
    public boolean execute() {
        return new RegisterCustomerCollaboratorUI().show();
    }
}