package airtransport.presentation.pilot;

import eapli.framework.actions.Action;

public class RemovePilotFromCompanyAction implements Action {

    @Override
    public boolean execute() {
        return new RemovePilotFromCompanyUI().show();
    }
}
