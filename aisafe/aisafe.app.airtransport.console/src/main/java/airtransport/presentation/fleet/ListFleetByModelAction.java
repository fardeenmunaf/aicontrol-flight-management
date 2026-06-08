package airtransport.presentation.fleet;

import eapli.framework.actions.Action;

public class ListFleetByModelAction implements Action {

    @Override
    public boolean execute() {
        new ListFleetByModelUI().show();
        return false;
    }
}
