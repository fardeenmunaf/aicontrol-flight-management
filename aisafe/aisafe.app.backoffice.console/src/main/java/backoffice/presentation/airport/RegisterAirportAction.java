package backoffice.presentation.airport;

import eapli.framework.actions.Action;

public class RegisterAirportAction implements Action {

    @Override
    public boolean execute() {
        return new RegisterAirportUI().show();
    }
}