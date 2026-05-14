package backoffice.presentation.customercollaborator;

import eapli.framework.actions.Action;

public class ListCustomerCollaboratorsAction implements Action {
    @Override
    public boolean execute() {
        new ListCustomerCollaboratorsUI().show();
        return false;
    }
}
