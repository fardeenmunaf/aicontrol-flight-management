package airtransport.presentation;

import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractUI {

    private static final int EXIT_OPTION = 0;
    private static final int FLEET_OPTION = 1;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu();
        menu.addItem(FLEET_OPTION, "Fleet Management", new FleetMenuAction());
        menu.addItem(EXIT_OPTION, "Exit", () -> true);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Air Transport Console";
    }
}
