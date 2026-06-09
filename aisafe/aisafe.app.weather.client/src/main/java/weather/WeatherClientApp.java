package weather;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;
import weather.presentation.WeatherMenu;
import weather.presentation.authz.WeatherLoginUI;


public final class WeatherClientApp {

    private WeatherClientApp() {
        // utility
    }

    public static void main(final String[] args) {
        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));

        System.out.println("Select Connection Mode:");
        System.out.println("1. TCP (Remote)");
        System.out.println("2. Direct Database (Local)");
        String choice = System.console().readLine("Choice: ");

        if ("2".equals(choice)) {
            ClientSession.setMode(ClientSession.ConnectionMode.DIRECT_DB);
        } else {
            ClientSession.setMode(ClientSession.ConnectionMode.TCP);
        }

        if (new WeatherLoginUI().show()) {
            new WeatherMenu().mainLoop();
        }
    }
}
