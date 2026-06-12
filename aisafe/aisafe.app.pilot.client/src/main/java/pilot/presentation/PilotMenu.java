package pilot.presentation;

import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;
import pilot.PilotClientSession;
import pilot.presentation.fleet.ListFleetRemoteUI;
import pilot.presentation.flightplan.AddWeatherDataRemoteUI;
import pilot.presentation.flightplan.CreateFlightPlanRemoteUI;
import pilot.presentation.flightplan.ListFlightPlansUI;
import pilot.presentation.flightplan.ValidateFlightPlanRemoteUI;
import pilot.presentation.flightroute.ListFlightRoutesRemoteUI;

import java.io.IOException;

public class PilotMenu extends AbstractUI {

    private static final int EXIT_OPTION          = 0;
    private static final int LIST_PLANS           = 1;
    private static final int CREATE_PLAN          = 2;
    private static final int VALIDATE_PLAN        = 3;
    private static final int ADD_WEATHER          = 4;
    private static final int LIST_FLEET           = 5;
    private static final int LIST_ROUTES          = 6;

    @Override
    protected boolean doShow() {
        final Menu menu = new Menu();
        menu.addItem(LIST_PLANS,    "View My Flight Plans",                () -> { new ListFlightPlansUI().show(); return false; });
        menu.addItem(CREATE_PLAN,   "Create Flight Plan (from DSL)",       () -> { new CreateFlightPlanRemoteUI().show(); return false; });
        menu.addItem(VALIDATE_PLAN, "Validate / Test Flight Plan",         () -> { new ValidateFlightPlanRemoteUI().show(); return false; });
        menu.addItem(ADD_WEATHER,   "Add Weather Data to Flight Plan",     () -> { new AddWeatherDataRemoteUI().show(); return false; });
        menu.addItem(LIST_FLEET,    "View My Certified Fleet",             () -> { new ListFleetRemoteUI().show(); return false; });
        menu.addItem(LIST_ROUTES,   "View Company Flight Routes",          () -> { new ListFlightRoutesRemoteUI().show(); return false; });
        menu.addItem(EXIT_OPTION,   "Exit", () -> {
            disconnectGracefully();
            return true;
        });
        final MenuRenderer renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private void disconnectGracefully() {
        try {
            final var client = PilotClientSession.getClient();
            if (client != null) client.close();
        } catch (final IOException ignored) {}
        System.out.println("Disconnected from server. Goodbye!");
    }

    @Override
    public String headline() {
        return "Pilot Remote Menu (US086)";
    }
}
