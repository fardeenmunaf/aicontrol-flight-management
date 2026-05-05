package backoffice.presentation.enginemodel;

import eapli.framework.actions.Action;

public class RegisterEngineModelAction implements Action {

    @Override
    public boolean execute() {
        new RegisterEngineModelUI().show();
        return false;
    }
}
