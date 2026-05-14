package airtransport.presentation.flightplan;

import eapli.framework.actions.Action;

public class ImportFlightPlanAction implements Action {

    @Override
    public boolean execute() {
        new ImportFlightPlanUI().show();
        return false;
    }
}
