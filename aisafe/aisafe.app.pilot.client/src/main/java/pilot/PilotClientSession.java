package pilot;

import pilot.server.PilotTcpClient;

public class PilotClientSession {

    private static PilotTcpClient client;

    public static void setClient(final PilotTcpClient c) {
        client = c;
    }

    public static PilotTcpClient getClient() {
        return client;
    }

    private PilotClientSession() {}
}
