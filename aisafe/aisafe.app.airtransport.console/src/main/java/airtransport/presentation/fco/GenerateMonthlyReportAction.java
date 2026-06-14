package airtransport.presentation.fco;

import eapli.framework.actions.Action;

public class GenerateMonthlyReportAction implements Action {

    @Override
    public boolean execute() {
        new GenerateMonthlyReportUI().show();
        return false;
    }
}
