package backoffice.presentation.aircraftmodel;

import eapli.framework.actions.Action;

public class RemoveEngineModelFromAircraftAction implements Action {

    @Override
    public boolean execute() {
        return new RemoveEngineModelFromAircraftUI().show();
    }
}