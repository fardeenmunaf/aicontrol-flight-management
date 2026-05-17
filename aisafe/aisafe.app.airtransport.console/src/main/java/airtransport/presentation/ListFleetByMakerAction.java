package airtransport.presentation;

import eapli.framework.actions.Action;

public class ListFleetByMakerAction implements Action {

    @Override
    public boolean execute() {
        new ListFleetByMakerUI().show();
        return false;
    }
}
