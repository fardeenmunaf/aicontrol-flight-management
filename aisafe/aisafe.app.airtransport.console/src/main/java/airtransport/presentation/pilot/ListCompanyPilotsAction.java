package airtransport.presentation.pilot;

import eapli.framework.actions.Action;

public class ListCompanyPilotsAction implements Action {

    @Override
    public boolean execute() {
        return new ListCompanyPilotsUI().show();
    }
}
