package backoffice.presentation.enginemodel;

import eapli.framework.actions.Action;

public class ListEngineModelsAction implements Action {

    @Override
    public boolean execute() {
        new ListEngineModelsUI().show();
        return false;
    }
}
