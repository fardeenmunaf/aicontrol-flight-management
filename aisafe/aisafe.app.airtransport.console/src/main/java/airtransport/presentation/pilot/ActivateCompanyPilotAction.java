package airtransport.presentation.pilot;

import eapli.framework.actions.Action;

public class ActivateCompanyPilotAction implements Action {

    @Override
    public boolean execute() {
        return new ActivateCompanyPilotUI().show();
    }
}
