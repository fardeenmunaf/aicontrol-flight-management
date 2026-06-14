package airtransport.presentation.fco;

import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

/**
 * FCO (Flight Control Operator) sub-menu.
 */
public class FcoMenuUI extends AbstractUI {

    private static final int MONTHLY_REPORT    = 1;
    private static final int RETURN            = 0;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("FCO Operations");
        menu.addItem(MONTHLY_REPORT, "Generate Monthly Statistics Report", new GenerateMonthlyReportAction());
        menu.addItem(RETURN, "Return", () -> false);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "FCO Operations";
    }
}
