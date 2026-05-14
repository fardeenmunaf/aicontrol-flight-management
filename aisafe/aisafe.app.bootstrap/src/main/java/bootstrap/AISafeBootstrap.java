package bootstrap;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import infrastructure.persistence.PersistenceContext;
import jpa.PersistenceSettings;

/**
 * Entry point for loading master data used in development and testing.
 */
public final class AISafeBootstrap {

    private AISafeBootstrap() {
        // utility class
    }

    public static void main(final String[] args) {
        System.out.println("Starting AISafe bootstrap...");

        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));
        final var repositories = PersistenceContext.repositories();

        new AirTransportCompanyBootstrap(repositories.airTransportCompanies()).execute();
        new UsersBootstrap(repositories.users()).execute();
        new EngineModelBootstrap(repositories.engineModels()).execute();
        new AircraftModelBootstrap(repositories.aircraftModels(), repositories.engineModels()).execute();
        new AirControlAreaBootstrap(repositories.airControlAreas()).execute();
        new AirportBootstrap(repositories.airports(), repositories.airControlAreas()).execute();
        new CustomerCollaboratorBootstrap(
                repositories.users(),
                repositories.customerCollaborators(),
                repositories.airTransportCompanies(),
                repositories.airControlAreas()
        ).execute();
        System.out.println("AISafe bootstrap completed.");
    }
}