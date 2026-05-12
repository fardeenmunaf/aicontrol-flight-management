package jpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    private static final String PROD_PERSISTENCE_UNIT = "aisafe-pu-postgres";
    private static final String AISAFE_DB_DIALECT = "aisafe.db.dialect";
    private static final String AISAFE_DB_DRIVER = "aisafe.db.driver";
    private static final String AISAFE_DB_URL = "aisafe.db.url";
    private static final String AISAFE_DB_USERNAME = "aisafe.db.username";
    private static final String AISAFE_DB_PASSWORD = "aisafe.db.password";
    private static final String PROFILE_ACTIVE_KEY = "spring.profiles.active";
    private static final String PROFILE_DEFAULT_KEY = "spring.profiles.default";
    private static final String PROFILE_DEV = "dev";
    private static final String PROFILE_PROD = "prod";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String APPLICATION_PROFILE_PREFIX = "application-";
    private static final String APPLICATION_PROFILE_SUFFIX = ".properties";
    private static final String SPRING_DS_DRIVER = "spring.datasource.driver-class-name";
    private static final String SPRING_DS_URL = "spring.datasource.url";
    private static final String SPRING_DS_USERNAME = "spring.datasource.username";
    private static final String SPRING_DS_PASSWORD = "spring.datasource.password";
    private static final String SPRING_JPA_DIALECT = "spring.jpa.properties.hibernate.dialect";
    private static final String SPRING_JPA_DDL = "spring.jpa.hibernate.ddl-auto";
    private static final String SPRING_JPA_SHOW_SQL = "spring.jpa.show-sql";
    private static final String SPRING_JPA_FORMAT_SQL = "spring.jpa.properties.hibernate.format_sql";
    private static final String SPRING_JPA_SQL_COMMENTS = "spring.jpa.properties.hibernate.use_sql_comments";
    private static final String SPRING_JPA_STATS = "spring.jpa.properties.hibernate.generate_statistics";
    private static final String SPRING_JPA_POOL = "spring.jpa.properties.hibernate.hikari.maximumPoolSize";

    private static final Properties BASE_PROPERTIES = loadProperties(APPLICATION_PROPERTIES);
    private static final String ACTIVE_PROFILE = resolveActiveProfile(BASE_PROPERTIES);
    private static final Properties PROFILE_PROPERTIES = loadProperties(APPLICATION_PROFILE_PREFIX
            + ACTIVE_PROFILE + APPLICATION_PROFILE_SUFFIX);

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
        final String explicitUnit = System.getProperty("aisafe.persistence.unit");
        if (explicitUnit != null && !explicitUnit.isBlank()) {
            return explicitUnit;
        }

        return PROFILE_PROD.equalsIgnoreCase(ACTIVE_PROFILE)
                ? PROD_PERSISTENCE_UNIT
                : DEFAULT_PERSISTENCE_UNIT;
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
        } else {
            putIfPresent(properties, "hibernate.dialect", getProperty(SPRING_JPA_DIALECT));
        }
        if (driver != null && !driver.isBlank()) {
            properties.put("jakarta.persistence.jdbc.driver", driver);
        } else {
            putIfPresent(properties, "jakarta.persistence.jdbc.driver", getProperty(SPRING_DS_DRIVER));
        }
        if (url != null && !url.isBlank()) {
            properties.put("jakarta.persistence.jdbc.url", url);
        } else {
            putIfPresent(properties, "jakarta.persistence.jdbc.url", getProperty(SPRING_DS_URL));
        }
        if (username != null && !username.isBlank()) {
            properties.put("jakarta.persistence.jdbc.user", username);
        } else {
            putIfPresent(properties, "jakarta.persistence.jdbc.user", getProperty(SPRING_DS_USERNAME));
        }
        if (password != null && !password.isBlank()) {
            properties.put("jakarta.persistence.jdbc.password", password);
        } else {
            putIfPresent(properties, "jakarta.persistence.jdbc.password", getProperty(SPRING_DS_PASSWORD));
        }

        putIfPresent(properties, "hibernate.hbm2ddl.auto", getProperty(SPRING_JPA_DDL));
        putIfPresent(properties, "hibernate.show_sql", getProperty(SPRING_JPA_SHOW_SQL));
        putIfPresent(properties, "hibernate.format_sql", getProperty(SPRING_JPA_FORMAT_SQL));
        putIfPresent(properties, "hibernate.use_sql_comments", getProperty(SPRING_JPA_SQL_COMMENTS));
        putIfPresent(properties, "hibernate.generate_statistics", getProperty(SPRING_JPA_STATS));
        putIfPresent(properties, "hibernate.hikari.maximumPoolSize", getProperty(SPRING_JPA_POOL));

        return properties;
    }

    private static void putIfPresent(final Map<String, Object> properties, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }

    private static String resolveActiveProfile(final Properties baseProperties) {
        final String explicitProfile = System.getProperty(PROFILE_ACTIVE_KEY);
        if (explicitProfile != null && !explicitProfile.isBlank()) {
            return explicitProfile.trim();
        }

        final String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (envProfile != null && !envProfile.isBlank()) {
            return envProfile.trim();
        }

        final String defaultProfile = baseProperties.getProperty(PROFILE_DEFAULT_KEY);
        if (defaultProfile != null && !defaultProfile.isBlank()) {
            return defaultProfile.trim();
        }

        final String envDefaultProfile = System.getenv("SPRING_PROFILES_DEFAULT");
        if (envDefaultProfile != null && !envDefaultProfile.isBlank()) {
            return envDefaultProfile.trim();
        }

        return PROFILE_DEV;
    }

    private static Properties loadProperties(final String resourceName) {
        final Properties properties = new Properties();
        try (InputStream input = PersistenceSettings.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load configuration: " + resourceName, ex);
        }
        return properties;
    }

    private static String getProperty(final String key) {
        final String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        final String profileValue = PROFILE_PROPERTIES.getProperty(key);
        if (profileValue != null && !profileValue.isBlank()) {
            return profileValue;
        }
        final String baseValue = BASE_PROPERTIES.getProperty(key);
        if (baseValue != null && !baseValue.isBlank()) {
            return baseValue;
        }
        return null;
    }
}
