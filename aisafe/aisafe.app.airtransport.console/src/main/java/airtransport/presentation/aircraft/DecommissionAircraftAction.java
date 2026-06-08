package airtransport.presentation.aircraft;

import eapli.framework.actions.Action;

public class DecommissionAircraftAction implements Action {
    @Override
    public boolean execute() {
        new DecommissionAircraftUI().show();
        return false;
    }
}
