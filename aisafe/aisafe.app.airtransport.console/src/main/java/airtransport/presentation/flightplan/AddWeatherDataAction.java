package airtransport.presentation.flightplan;

import eapli.framework.actions.Action;

public class AddWeatherDataAction implements Action {

    @Override
    public boolean execute() {
        return new AddWeatherDataUI().show();
    }
}
