package airtransport.presentation.aircraft;

import eapli.framework.actions.Action;

public class ListAircraftAction implements Action {
    @Override
    public boolean execute() {
        new ListAircraftUI().show();
        return false;
    }
}
