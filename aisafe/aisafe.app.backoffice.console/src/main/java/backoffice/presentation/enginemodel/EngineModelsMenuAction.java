package backoffice.presentation.enginemodel;

import eapli.framework.actions.Action;

public class EngineModelsMenuAction implements Action {

    @Override
    public boolean execute() {
        new EngineModelsMenuUI().mainLoop();
        return false;
    }
}
