package jpa;

import java.util.HashMap;
import java.util.Map;

import eapli.framework.util.Utility;

@Utility
public final class PersistenceSettings {
    private static final String DEFAULT_PERSISTENCE_UNIT = "aisafe-pu";
    private static final String AISAFE_DB_DIALECT = "aisafe.db.dialect";
    private static final String AISAFE_DB_DRIVER = "aisafe.db.driver";
    private static final String AISAFE_DB_URL = "aisafe.db.url";
    private static final String AISAFE_DB_USERNAME = "aisafe.db.username";
    private static final String AISAFE_DB_PASSWORD = "aisafe.db.password";

    // H2 in-memory defaults
    private static final String DEFAULT_DIALECT  = "org.hibernate.dialect.H2Dialect";
    private static final String DEFAULT_DRIVER   = "org.h2.Driver";
    private static final String DEFAULT_URL = "jdbc:h2:tcp://localhost/~/aisafe-dev;NON_KEYWORDS=VALUE";
    private static final String DEFAULT_USERNAME = "sa";
    private static final String DEFAULT_PASSWORD = "";

    private PersistenceSettings() {}

    public static String getPersistenceUnitName() {
        return System.getProperty("aisafe.persistence.unit", DEFAULT_PERSISTENCE_UNIT);
    }

    public static Map<String, Object> getExtendedPersistenceProperties() {
        final Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.dialect",              getOrDefault(AISAFE_DB_DIALECT,  DEFAULT_DIALECT));
        properties.put("jakarta.persistence.jdbc.driver", getOrDefault(AISAFE_DB_DRIVER,  DEFAULT_DRIVER));
        properties.put("jakarta.persistence.jdbc.url",   getOrDefault(AISAFE_DB_URL,      DEFAULT_URL));
        properties.put("jakarta.persistence.jdbc.user",  getOrDefault(AISAFE_DB_USERNAME, DEFAULT_USERNAME));
        properties.put("jakarta.persistence.jdbc.password", System.getProperty(AISAFE_DB_PASSWORD, DEFAULT_PASSWORD));

        return properties;
    }

    private static String getOrDefault(String propertyKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}