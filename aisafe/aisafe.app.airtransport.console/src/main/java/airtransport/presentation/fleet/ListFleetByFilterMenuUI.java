package airtransport.presentation.fleet;

import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class ListFleetByFilterMenuUI extends AbstractUI {

    private static final int BY_MODEL = 1;
    private static final int BY_MAKER = 2;
    private static final int BY_CAPACITY = 3;
    private static final int RETURN = 0;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu("List Fleet By Filter");
        menu.addItem(BY_MODEL, "By Model", new ListFleetByModelAction());
        menu.addItem(BY_MAKER, "By Maker", new ListFleetByMakerAction());
        menu.addItem(BY_CAPACITY, "By Capacity (Number of Passengers)", new ListFleetByCapacityAction());
        menu.addItem(RETURN, "Return", () -> false);
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "List Fleet By Filter";
    }
}
