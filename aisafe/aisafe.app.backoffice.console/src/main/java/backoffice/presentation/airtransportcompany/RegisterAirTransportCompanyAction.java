package backoffice.presentation.airtransportcompany;

import eapli.framework.actions.Action;

public class RegisterAirTransportCompanyAction implements Action {

    @Override
    public boolean execute() {
        new RegisterAirTransportCompanyUI().show();
        return false;
    }
}

