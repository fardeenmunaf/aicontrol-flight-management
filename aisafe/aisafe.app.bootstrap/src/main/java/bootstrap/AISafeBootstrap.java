package bootstrap;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import infrastructure.persistence.PersistenceContext;

/**
 * Entry point for loading master data used in development and testing.
 */
public final class AISafeBootstrap {

    private AISafeBootstrap() {
        // utility class
    }

    public static void main(final String[] args) {
        System.out.println("Starting AISafe bootstrap...");

        AuthzConfigurator.configure(new InMemoryUserRepository());
        final var repositories = PersistenceContext.repositories();

        new UsersBootstrap(repositories.users()).execute();
        new EngineModelBootstrap(repositories.engineModels()).execute();

        System.out.println("AISafe bootstrap completed.");
    }
}
