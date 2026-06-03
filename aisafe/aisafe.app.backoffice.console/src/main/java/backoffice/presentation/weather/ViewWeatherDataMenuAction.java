package backoffice.presentation.weather;

import eapli.framework.actions.Action;

public class ViewWeatherDataMenuAction implements Action {

    @Override
    public boolean execute() {
        new ViewWeatherDataByDayAreaUI().show();
        return false;
    }
}


