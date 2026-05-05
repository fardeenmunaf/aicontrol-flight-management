package backoffice.presentation.enginemodel;

import eapli.framework.actions.Action;

public class DeleteEngineModelAction implements Action {

    @Override
    public boolean execute() {
        new DeleteEngineModelUI().show();
        return false;
    }
}
