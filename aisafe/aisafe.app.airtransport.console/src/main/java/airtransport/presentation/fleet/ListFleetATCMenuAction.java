package airtransport.presentation.fleet;

import eapli.framework.actions.Action;

public class ListFleetATCMenuAction implements Action {

    @Override
    public boolean execute() {
        new ListFleetATCUI().show();
        return false;
    }
}



