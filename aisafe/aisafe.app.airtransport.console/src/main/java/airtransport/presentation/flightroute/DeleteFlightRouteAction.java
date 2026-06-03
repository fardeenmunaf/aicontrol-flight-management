package airtransport.presentation.flightroute;

import eapli.framework.actions.Action;

public class DeleteFlightRouteAction implements Action {

    @Override
    public boolean execute() {
        new DeleteFlightRouteUI().show();
        return false;
    }
}
