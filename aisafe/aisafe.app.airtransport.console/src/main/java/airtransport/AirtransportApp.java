package airtransport;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import airtransport.presentation.MainMenu;
import airtransport.presentation.authz.LoginUI;
import jpa.PersistenceSettings;

public final class AirtransportApp {

	private AirtransportApp() {
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
