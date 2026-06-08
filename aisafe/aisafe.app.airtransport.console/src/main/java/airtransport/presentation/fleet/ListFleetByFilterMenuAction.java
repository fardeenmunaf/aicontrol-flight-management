package airtransport.presentation.fleet;

import eapli.framework.actions.Action;

public class ListFleetByFilterMenuAction implements Action {

    @Override
    public boolean execute() {
        return new ListFleetByFilterMenuUI().show();
    }
}
