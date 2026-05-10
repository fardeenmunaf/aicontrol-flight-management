package backoffice.presentation;

import auth.domain.AISafeRoles;
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

    private static final int USERS_OPTION = 1;
    private static final int ENGINE_MODELS_OPTION = 2;
    private static final int AIRCRAFT_MODELS_OPTION = 3;
    private static final int AIRPORTS_OPTION = 4;

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

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.ADMIN)) {
            mainMenu.addItem(USERS_OPTION, "Users Management",
                    new backoffice.presentation.user.UsersMenuAction());
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.BACKOFFICE_OPERATOR)) {
            mainMenu.addItem(ENGINE_MODELS_OPTION, "Engine Models",
                    new backoffice.presentation.enginemodel.EngineModelsMenuAction());
            mainMenu.addItem(AIRCRAFT_MODELS_OPTION, "Aircraft Models",
                    new backoffice.presentation.aircraftmodel.AircraftModelsMenuAction());
            mainMenu.addItem(AIRPORTS_OPTION, "Airports",
                    new backoffice.presentation.airport.AirportsMenuAction());
        }

        mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Thank you for using the AISafe Backoffice."));
        return mainMenu;
    }
}
