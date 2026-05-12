package backoffice;

import auth.infrastructure.AuthzConfigurator;
import backoffice.presentation.MainMenu;
import backoffice.presentation.authz.LoginUI;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;

public final class BackofficeApp {

    private BackofficeApp() {
        // utility
    }

    public static void main(final String[] args) {
        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));

        if (new LoginUI().show()) {
            new MainMenu().mainLoop();
        }
    }
}
