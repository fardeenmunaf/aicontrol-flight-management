package backoffice;

import auth.infrastructure.AuthzConfigurator;
import backoffice.presentation.MainMenu;
import backoffice.presentation.authz.LoginUI;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;


public final class BackofficeApp {

    private BackofficeApp() {
        // utility
    }

    public static void main(final String[] args) {
        AuthzConfigurator.configure(new InMemoryUserRepository());

        if (new LoginUI().show()) {
            new MainMenu().mainLoop();
        }
    }
}

