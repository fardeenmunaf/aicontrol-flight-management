package infrastructure.persistence;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.framework.util.Utility;

/**
 * Provides centralized access to the persistence layer.
 * Works as a factory of factories, enabling dynamic switching between
 * JPA and In-Memory implementations based on system configuration.
 *
 * @author LAPR4 Team
 */
@Utility
public final class PersistenceContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceContext.class);

    private static volatile RepositoryFactory theFactory;

    private PersistenceContext() {
        // ensure utility
    }

    /**
     * Get the current repository factory instance.
     * Dynamically loads the factory class specified in configuration on first access.
     *
     * @return the active RepositoryFactory
     * @throws IllegalStateException if factory cannot be dynamically loaded
     */
    public static synchronized RepositoryFactory repositories() {
        if (theFactory == null) {
            final String factoryClassName = getRepositoryFactoryClassName();
            try {
                theFactory = (RepositoryFactory) Class.forName(factoryClassName)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException | ClassNotFoundException ex) {
                LOGGER.error("Unable to dynamically load the Repository Factory", ex);
                throw new IllegalStateException(
                        "Unable to dynamically load the Repository Factory: " + factoryClassName, ex);
            }
        }
        return theFactory;
    }

    /**
     * Retrieve the repository factory class name from system configuration.
     * Defaults to InMemoryRepositoryFactory if not configured.
     *
     * @return the fully qualified class name of the RepositoryFactory implementation
     */
    private static String getRepositoryFactoryClassName() {
        // Try to get from system property first
        String factoryClass = System.getProperty("aisafe.persistence.factory");
        if (factoryClass != null && !factoryClass.isBlank()) {
            return factoryClass;
        }
        // Default to JPA for development with local file H2 DB
        return "jpa.JpaRepositoryFactory";
    }

    /**
     * Reset the factory instance (useful for testing).
     */
    protected static void reset() {
        theFactory = null;
    }
}
