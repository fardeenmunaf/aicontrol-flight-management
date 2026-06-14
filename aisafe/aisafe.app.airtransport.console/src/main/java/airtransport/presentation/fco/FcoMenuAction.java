package airtransport.presentation.fco;

import eapli.framework.actions.Action;

public class FcoMenuAction implements Action {

    @Override
    public boolean execute() {
        new FcoMenuUI().show();
        return false;
    }
}
