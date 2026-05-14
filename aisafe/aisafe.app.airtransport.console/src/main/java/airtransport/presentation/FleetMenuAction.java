package airtransport.presentation;

import eapli.framework.actions.Action;
import eapli.framework.presentation.console.AbstractUI;

public class FleetMenuAction implements Action {

    @Override
    public boolean execute() {
        return new FleetMenuUI().show();
    }
}
