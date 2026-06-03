package airtransport.presentation.flightroute;

import eapli.framework.actions.Action;

public class CreateFlightRouteAction implements Action {

    @Override
    public boolean execute() {
        new CreateFlightRouteUI().show();
        return false;
    }
}
