package backoffice.presentation.aircontrolarea;

import eapli.framework.actions.Action;

public class RegisterAirControlAreaAction implements Action {

    @Override
    public boolean execute() {
        new RegisterAirControlAreaUI().show();
        return false;
    }
}