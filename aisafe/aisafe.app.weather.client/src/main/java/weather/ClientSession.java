package weather;
import weather.server.WeatherTcpClient;

public class ClientSession {
    public enum ConnectionMode {
        TCP, DIRECT_DB
    }

    private static WeatherTcpClient client;
    private static ConnectionMode mode;

    public static void setClient(WeatherTcpClient c) {
        client = c;
    }

    public static WeatherTcpClient getClient() {
        return client;
    }

    public static void setMode(ConnectionMode m) {
        mode = m;
    }

    public static ConnectionMode getMode() {
        return mode;
    }
}
