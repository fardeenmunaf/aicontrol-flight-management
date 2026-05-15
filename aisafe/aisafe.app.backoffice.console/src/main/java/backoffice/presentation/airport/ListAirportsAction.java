package backoffice.presentation.airport;

import eapli.framework.actions.Action;

public class ListAirportsAction implements Action {

    @Override
    public boolean execute() {
        return new ListAirportsUI().show();
    }
}
