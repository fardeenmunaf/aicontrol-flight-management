package backoffice.presentation.enginemodel;

import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class EngineModelsMenuUI extends AbstractUI {

    private static final int RETURN_OPTION = 0;
    private static final int REGISTER_ENGINE_MODEL_OPTION = 1;
    private static final int LIST_ENGINE_MODELS_OPTION = 2;
    private static final int DELETE_ENGINE_MODEL_OPTION = 3;

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final Menu menu = buildEngineModelsMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildEngineModelsMenu() {
        final Menu menu = new Menu("Engine Models >");

        menu.addItem(REGISTER_ENGINE_MODEL_OPTION, "Register Engine Model", new RegisterEngineModelAction());
        menu.addItem(LIST_ENGINE_MODELS_OPTION, "List Engine Models", new ListEngineModelsAction());
        menu.addItem(DELETE_ENGINE_MODEL_OPTION, "Delete Engine Model", new DeleteEngineModelAction());
        menu.addItem(RETURN_OPTION, "Return", Actions.SUCCESS);

        return menu;
    }

    @Override
    public String headline() {
        return "Engine Models Management";
    }
}
