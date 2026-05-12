package backoffice.presentation.customercollaborator;

import eapli.framework.actions.Action;

/**
 * Action for updating a customer collaborator's contact information.
 */
public class UpdateCustomerCollaboratorAction implements Action {

    @Override
    public boolean execute() {
        return new UpdateCustomerCollaboratorUI().show();
    }
}
