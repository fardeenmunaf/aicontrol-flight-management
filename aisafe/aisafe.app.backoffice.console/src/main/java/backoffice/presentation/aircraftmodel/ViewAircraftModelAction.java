package backoffice.presentation.aircraftmodel;

import eapli.framework.actions.Action;


public class ViewAircraftModelAction implements Action{

    @Override
        public boolean execute() {
            new ViewAircraftModelUI().show();
            return false;
    }
}
