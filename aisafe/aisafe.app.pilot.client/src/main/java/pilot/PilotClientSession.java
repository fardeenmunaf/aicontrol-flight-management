package pilot;

import pilot.server.PilotTcpClient;

public class PilotClientSession {

    private static PilotTcpClient client;
    private static String pilotEmail;

    public static void setClient(final PilotTcpClient c, final String email) {
        client = c;
        pilotEmail = email;
    }

    public static PilotTcpClient getClient() {
        return client;
    }

    public static String getPilotEmail() {
        return pilotEmail != null ? pilotEmail : "";
    }

    private PilotClientSession() {}
}
