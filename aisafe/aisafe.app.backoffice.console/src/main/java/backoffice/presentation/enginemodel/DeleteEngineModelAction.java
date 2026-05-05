package backoffice.presentation.enginemodel;

import eapli.framework.actions.Action;

public class DeleteEngineModelAction implements Action {

    @Override
    public boolean execute() {
        return new DeleteEngineModelUI().show();
    }
}
