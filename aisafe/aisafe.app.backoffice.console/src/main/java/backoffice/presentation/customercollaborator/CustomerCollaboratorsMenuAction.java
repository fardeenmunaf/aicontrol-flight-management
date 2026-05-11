package backoffice.presentation.customercollaborator;

import eapli.framework.actions.Action;

public class CustomerCollaboratorsMenuAction implements Action {
    @Override
    public boolean execute() {
        return new CustomerCollaboratorsMenu().show();
    }
}