package airtransport.presentation.pilot;

import eapli.framework.actions.Action;

public class PilotManagementMenuAction implements Action {

    @Override
    public boolean execute() {
        new PilotManagementMenuUI().mainLoop();
        return false;
    }
}
