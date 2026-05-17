package airtransport.presentation;

import eapli.framework.actions.Action;

public class ListFleetByModelAction implements Action {

    @Override
    public boolean execute() {
        new ListFleetByModelUI().show();
        return false;
    }
}
