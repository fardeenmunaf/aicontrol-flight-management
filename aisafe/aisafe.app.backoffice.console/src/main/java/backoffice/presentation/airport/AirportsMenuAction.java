package backoffice.presentation.airport;

import eapli.framework.actions.Action;

public class AirportsMenuAction implements Action {

    @Override
    public boolean execute() {
        return new AirportsMenu().show();
    }
}