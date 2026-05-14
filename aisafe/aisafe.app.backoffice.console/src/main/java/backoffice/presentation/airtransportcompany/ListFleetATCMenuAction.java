package backoffice.presentation.airtransportcompany;

import eapli.framework.actions.Action;

public class ListFleetATCMenuAction implements Action {

    @Override
    public boolean execute() {
        new ListFleetATCUI().show();
        return false;
    }
}


