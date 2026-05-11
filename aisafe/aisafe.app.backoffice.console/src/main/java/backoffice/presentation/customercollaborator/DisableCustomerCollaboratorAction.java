package backoffice.presentation.customercollaborator;

import eapli.framework.actions.Action;

public class DisableCustomerCollaboratorAction implements Action {
    @Override
    public boolean execute() {
        return new DisableCustomerCollaboratorUI().show();
    }
}