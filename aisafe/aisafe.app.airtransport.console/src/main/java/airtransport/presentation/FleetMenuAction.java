package airtransport.presentation;

import eapli.framework.actions.Action;

public class FleetMenuAction implements Action {

    @Override
    public boolean execute() {
        return new FleetMenuUI().show();
    }
}
