package jpa;

import eapli.framework.infrastructure.repositories.impl.jpa.JpaTransactionalContext;
import jakarta.persistence.EntityManagerFactory;

/**
 * Bridge to the {@link EntityManagerFactory} that the eapli framework keeps as
 * a process-wide singleton.
 *
 * <p>The framework only exposes the shared factory through the {@code protected}
 * {@link JpaTransactionalContext#entityManagerFactory()} method, so subclassing
 * it is the cleanest way to obtain (and close) that factory without resorting to
 * reflection.</p>
 */
final class SharedEntityManagerFactoryAccess extends JpaTransactionalContext {

    private SharedEntityManagerFactoryAccess() {
        super(PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties());
    }

    /**
     * Closes the shared {@link EntityManagerFactory} (and the underlying
     * connection pool) if it is currently open. Safe to call more than once.
     */
    static void closeSharedFactory() {
        final EntityManagerFactory emf = new SharedEntityManagerFactoryAccess().entityManagerFactory();
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
