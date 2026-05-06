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

        if (new WeatherLoginUI().show()) {
            new WeatherMenu().mainLoop();
        }
    }
}
