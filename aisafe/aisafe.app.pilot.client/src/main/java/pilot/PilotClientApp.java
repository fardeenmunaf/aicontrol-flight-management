package pilot;

import pilot.presentation.PilotMenu;
import pilot.presentation.authz.PilotLoginUI;

/**
 * US086 — Pilot Remote Access Client Application.
 *
 * Connects to the PilotTcpServer via TCP. No direct database access.
 * Authentication and authorization are enforced server-side.
 *
 * Usage:
 *   java -jar aisafe.app.pilot.client.jar [host] [port]
 *   Defaults: host=localhost, port=9001
 */
public final class PilotClientApp {

    private PilotClientApp() {}

    public static void main(final String[] args) {
        final String host = args.length > 0 ? args[0] : resolveHost();
        final int    port = args.length > 1 ? Integer.parseInt(args[1]) : resolvePort();

        System.out.println("=== AISafe Pilot Remote Access Client (US086) ===");
        System.out.println("Connecting to " + host + ":" + port);

        if (new PilotLoginUI(host, port).show()) {
            new PilotMenu().mainLoop();
        } else {
            System.out.println("Login failed. Exiting.");
        }
    }

    private static String resolveHost() {
        final String prop = System.getProperty("aisafe.pilot.host");
        if (prop != null && !prop.isBlank()) return prop;
        final String env = System.getenv("AISAFE_PILOT_HOST");
        if (env != null && !env.isBlank()) return env;
        return "localhost";
    }

    private static int resolvePort() {
        try {
            final String prop = System.getProperty("aisafe.pilot.port");
            if (prop != null && !prop.isBlank()) return Integer.parseInt(prop);
            final String env = System.getenv("AISAFE_PILOT_PORT");
            if (env != null && !env.isBlank()) return Integer.parseInt(env);
        } catch (final NumberFormatException ignored) {}
        return 9001;
    }
}
