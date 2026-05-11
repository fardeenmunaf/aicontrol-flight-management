package backoffice.presentation.airtransportcompany;

import eapli.framework.actions.Action;

public class AirTransportCompaniesMenuAction implements Action {

    @Override
    public boolean execute() {
        new AirTransportCompaniesMenuUI().mainLoop();
        return false;
    }
}

