package backoffice.presentation.aircraftmodel;

import eapli.framework.actions.Action;

public class AircraftModelsMenuAction implements Action {

    @Override
    public boolean execute() {
        new AircraftModelsMenuUI().mainLoop();
        return false;
    }
}

