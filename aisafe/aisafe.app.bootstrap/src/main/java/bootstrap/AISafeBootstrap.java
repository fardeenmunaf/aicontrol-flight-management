package bootstrap;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;
import jpa.ProfileSettings;
import infrastructure.persistence.PersistenceContext;

/**
 * Entry point for loading master data used in development and testing.
 */
public final class AISafeBootstrap {

    private AISafeBootstrap() {
        // utility class
    }

    public static void main(final String[] args) {
        ProfileSettings.applyFromArgs(args);
        System.out.println("Starting AISafe bootstrap...");

        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));
        final var repositories = PersistenceContext.repositories();

        new UsersBootstrap(repositories.users()).execute();
        new EngineModelBootstrap(repositories.engineModels()).execute();
        new AirTransportCompanyBootstrap(repositories.airTransportCompanies()).execute();
        new CustomerCollaboratorBootstrap(
                repositories.users(),
                repositories.customerCollaborators(),
                repositories.airTransportCompanies(),
                repositories.airControlAreas()
        ).execute();

        System.out.println("AISafe bootstrap completed.");
    }
}