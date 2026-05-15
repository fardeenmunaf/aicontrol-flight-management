package airtransport.presentation.flightplan;

import eapli.framework.actions.Action;

public class CreateFlightPlanAction implements Action {

    @Override
    public boolean execute() {
        new CreateFlightPlanUI().show();
        return false;
    }
}
