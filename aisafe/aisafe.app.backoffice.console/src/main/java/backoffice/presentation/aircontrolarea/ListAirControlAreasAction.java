package backoffice.presentation.aircontrolarea;

import eapli.framework.actions.Action;

public class ListAirControlAreasAction implements Action {

    @Override
    public boolean execute() {
        new ListAirControlAreasUI().show();
        return false;
    }
}