package javafxui;

public final class SessionContext {

    private static boolean authenticated = false;

    private SessionContext() {}

    public static void setAuthenticated(final boolean auth) {
        authenticated = auth;
    }

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static void clear() {
        authenticated = false;
    }
}
