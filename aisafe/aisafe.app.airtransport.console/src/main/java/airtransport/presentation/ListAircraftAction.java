package airtransport.presentation;

import eapli.framework.actions.Action;

public class ListAircraftAction implements Action {
    @Override
    public boolean execute() {
        new ListAircraftUI().show();
        return false;
    }
}
