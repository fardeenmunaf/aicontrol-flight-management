package airtransport.presentation;

import eapli.framework.actions.Action;

public class ListFleetByCapacityAction implements Action {

    @Override
    public boolean execute() {
        new ListFleetByCapacityUI().show();
        return false;
    }
}
