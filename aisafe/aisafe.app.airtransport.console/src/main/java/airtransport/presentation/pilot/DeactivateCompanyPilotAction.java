package airtransport.presentation.pilot;

import eapli.framework.actions.Action;

public class DeactivateCompanyPilotAction implements Action {

    @Override
    public boolean execute() {
        return new DeactivateCompanyPilotUI().show();
    }
}
