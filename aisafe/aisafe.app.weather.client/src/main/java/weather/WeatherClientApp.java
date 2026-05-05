package weather;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import weather.presentation.WeatherMenu;
import weather.presentation.authz.WeatherLoginUI;


public final class WeatherClientApp {

    private WeatherClientApp() {
        // utility
    }

    public static void main(final String[] args) {
        AuthzConfigurator.configure(new InMemoryUserRepository());

        if (new WeatherLoginUI().show()) {
            new WeatherMenu().mainLoop();
        }
    }
}
