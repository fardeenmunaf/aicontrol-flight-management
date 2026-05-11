package backoffice.presentation.airtransportcompany;

import eapli.framework.actions.Action;

public class ListAirTransportCompaniesAction implements Action {

    @Override
    public boolean execute() {
        new ListAirTransportCompaniesUI().show();
        return false;
    }
}

