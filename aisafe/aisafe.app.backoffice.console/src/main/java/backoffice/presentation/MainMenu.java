package backoffice.presentation;

import auth.domain.AISafeRoles;
import backoffice.presentation.enginemodel.RegisterEngineModelAction;
import backoffice.presentation.airport.RegisterAirportAction;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;


public class MainMenu extends AbstractUI {

    private static final String RETURN_LABEL = "Return";
    private static final int EXIT_OPTION = 0;

    private static final int ENGINE_MODELS_OPTION = 1;

    private static final int REGISTER_ENGINE_MODEL_OPTION = 1;
    private static final int LIST_ENGINE_MODELS_OPTION = 2;
    private static final int DELETE_ENGINE_MODEL_OPTION = 3;
    private static final int AIRPORTS_OPTION = 2;
    private static final int REGISTER_AIRPORT_OPTION = 1;

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    public void mainLoop() {
        boolean exit;
        do {
            exit = doShow();
        } while (!exit);
    }

    @Override
    protected boolean doShow() {
        final var menu = buildMainMenu();
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return authz.session()
                .map(s -> "Backoffice [ @" + s.authenticatedUser().identity() + " ]")
                .orElse("Backoffice [ ==Anonymous== ]");
    }

    private Menu buildMainMenu() {
        final var mainMenu = new Menu();

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.BACKOFFICE_OPERATOR)) {
            mainMenu.addSubMenu(ENGINE_MODELS_OPTION, buildEngineModelsMenu());
            mainMenu.addSubMenu(AIRPORTS_OPTION, buildAirportsMenu());
            mainMenu.addItem(ENGINE_MODELS_OPTION, "Engine Models",
                    new backoffice.presentation.enginemodel.EngineModelsMenuAction());
        }

        mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Thank you for using the AISafe Backoffice."));
        return mainMenu;
    }

    private Menu buildEngineModelsMenu() {
        final var menu = new Menu("Engine Models >");

        menu.addItem(REGISTER_ENGINE_MODEL_OPTION, "Register Engine Model", new RegisterEngineModelAction());
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);

        return menu;
    }
    private Menu buildAirportsMenu() {
        final var menu = new Menu("Airports >");
        menu.addItem(REGISTER_AIRPORT_OPTION, "Register Airport", new RegisterAirportAction());
        menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
        return menu;
    }
}
