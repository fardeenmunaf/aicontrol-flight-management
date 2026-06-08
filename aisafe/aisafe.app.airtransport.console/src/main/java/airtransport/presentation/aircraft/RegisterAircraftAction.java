package airtransport.presentation.aircraft;

import eapli.framework.actions.Action;

public class RegisterAircraftAction implements Action {
    @Override
    public boolean execute() {
        new RegisterAircraftUI().show();
        return false;
    }
}
