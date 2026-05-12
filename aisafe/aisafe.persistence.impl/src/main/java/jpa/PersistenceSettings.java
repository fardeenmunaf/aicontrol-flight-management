package jpa;

import java.util.HashMap;
import java.util.Map;

import eapli.framework.util.Utility;

/**
 * Handles loading of database-specific properties from application configuration.
 * Manages JPA persistence unit configuration and extended properties.
 *
 * @author LAPR4 Team
 */
@Utility
public final class PersistenceSettings {
    private static final String DEFAULT_PERSISTENCE_UNIT = "aisafe-pu";
    private static final String AISAFE_DB_DIALECT = "aisafe.db.dialect";
    private static final String AISAFE_DB_DRIVER = "aisafe.db.driver";
    private static final String AISAFE_DB_URL = "aisafe.db.url";
    private static final String AISAFE_DB_USERNAME = "aisafe.db.username";
    private static final String AISAFE_DB_PASSWORD = "aisafe.db.password";

    private PersistenceSettings() {
        // ensure utility
    }

    /**
     * Get the JPA persistence unit name.
     * Loads from system properties or defaults to "aisafe-pu".
     *
     * @return the persistence unit name
     */
    public static String getPersistenceUnitName() {
        return System.getProperty("aisafe.persistence.unit", DEFAULT_PERSISTENCE_UNIT);
    }

    /**
     * Get extended persistence properties for JPA configuration.
     * These properties override or supplement the persistence.xml configuration.
     *
     * @return a map of JPA properties
     */
    public static Map<String, Object> getExtendedPersistenceProperties() {
        final Map<String, Object> properties = new HashMap<>();

        // Get database configuration from system properties
        String dialect = System.getProperty(AISAFE_DB_DIALECT);
        String driver = System.getProperty(AISAFE_DB_DRIVER);
        String url = System.getProperty(AISAFE_DB_URL);
        String username = System.getProperty(AISAFE_DB_USERNAME);
        String password = System.getProperty(AISAFE_DB_PASSWORD);

        // Apply properties if configured
        if (dialect != null && !dialect.isBlank()) {
            properties.put("hibernate.dialect", dialect);
        }
        if (driver != null && !driver.isBlank()) {
            properties.put("jakarta.persistence.jdbc.driver", driver);
        }
        if (url != null && !url.isBlank()) {
            properties.put("jakarta.persistence.jdbc.url", url);
        }
        if (username != null && !username.isBlank()) {
            properties.put("jakarta.persistence.jdbc.user", username);
        }
        if (password != null && !password.isBlank()) {
            properties.put("jakarta.persistence.jdbc.password", password);
        }

        return properties;
    }
}
