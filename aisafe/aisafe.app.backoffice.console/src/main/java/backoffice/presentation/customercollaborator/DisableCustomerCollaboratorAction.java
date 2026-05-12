package backoffice.presentation.customercollaborator;

import eapli.framework.actions.Action;

public class DisableCustomerCollaboratorAction implements Action {
    @Override
    public boolean execute() {
        new DisableCustomerCollaboratorUI().show();
        return false;
    }
}
