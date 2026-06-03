package airtransport.presentation.pilot;

import eapli.framework.actions.Action;

public class AddPilotToCompanyAction implements Action {

    @Override
    public boolean execute() {
        return new AddPilotToCompanyUI().show();
    }
}
