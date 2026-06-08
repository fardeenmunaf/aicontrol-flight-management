package airtransport.presentation.fleet;

import eapli.framework.actions.Action;

public class FleetMenuAction implements Action {

    @Override
    public boolean execute() {
        return new FleetMenuUI().show();
    }
}
