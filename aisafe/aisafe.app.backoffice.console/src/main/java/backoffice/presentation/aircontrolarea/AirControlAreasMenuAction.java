package backoffice.presentation.aircontrolarea;

import eapli.framework.actions.Action;

public class AirControlAreasMenuAction implements Action {

    @Override
    public boolean execute() {
        new AirControlAreasMenuUI().mainLoop();
        return false;
    }
}