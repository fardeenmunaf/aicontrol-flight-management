package jpa;

import eapli.framework.util.Utility;

/**
 * Applies Spring-style profile arguments to system properties.
 */
@Utility
public final class ProfileSettings {

    private ProfileSettings() {
        // utility
    }

    public static void applyFromArgs(final String[] args) {
        if (args == null) {
            return;
        }

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg == null || arg.isBlank()) {
                continue;
            }
            if (arg.startsWith("--spring.profiles.active=")) {
                setIfPresent("spring.profiles.active", arg.substring("--spring.profiles.active=".length()));
                continue;
            }
            if (arg.startsWith("--spring.profiles.default=")) {
                setIfPresent("spring.profiles.default", arg.substring("--spring.profiles.default=".length()));
                continue;
            }
            if ("--spring.profiles.active".equals(arg) && i + 1 < args.length) {
                setIfPresent("spring.profiles.active", args[++i]);
                continue;
            }
            if ("--spring.profiles.default".equals(arg) && i + 1 < args.length) {
                setIfPresent("spring.profiles.default", args[++i]);
            }
        }
    }

    private static void setIfPresent(final String key, final String value) {
        if (value != null && !value.isBlank()) {
            System.setProperty(key, value.trim());
        }
    }
}

