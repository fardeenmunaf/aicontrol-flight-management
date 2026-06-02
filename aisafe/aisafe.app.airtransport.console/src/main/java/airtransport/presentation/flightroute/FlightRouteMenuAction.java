package airtransport.presentation.flightroute;

import eapli.framework.actions.Action;

public class FlightRouteMenuAction implements Action {

    @Override
    public boolean execute() {
        new FlightRouteMenuUI().show();
        return false;
    }
}
