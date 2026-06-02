package javafxui;

public final class SessionContext {

    private static boolean authenticated = false;
    private static boolean weatherClient = false;

    private SessionContext() {}

    public static void setAuthenticated(final boolean auth) {
        authenticated = auth;
        if (!auth) {
            weatherClient = false;
        }
    }

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static void setWeatherClient(final boolean enabled) {
        weatherClient = enabled;
    }

    public static boolean isWeatherClient() {
        return weatherClient;
    }

    public static void clear() {
        authenticated = false;
        weatherClient = false;
    }
}
