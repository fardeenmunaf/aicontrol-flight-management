package backoffice.presentation.aircraftmodel;

import eapli.framework.actions.Action;

public class AddEngineModelToAircraftAction implements Action {

    @Override
    public boolean execute() {
        new AddEngineModelToAircraftUI().show();
        return false;
    }
}

