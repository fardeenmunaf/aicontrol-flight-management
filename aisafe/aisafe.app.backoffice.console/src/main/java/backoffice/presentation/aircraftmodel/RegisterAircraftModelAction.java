package backoffice.presentation.aircraftmodel;

import eapli.framework.actions.Action;

public class RegisterAircraftModelAction implements Action {

    @Override
    public boolean execute() {
        new RegisterAircraftModelUI().show();
        return false;
    }
}

