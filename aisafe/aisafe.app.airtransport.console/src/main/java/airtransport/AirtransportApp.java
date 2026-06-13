package airtransport;

import airtransport.presentation.MainMenu;
import airtransport.presentation.authz.LoginUI;
import airtransport.presentation.authz.TcpLoginUI;
import airtransport.tcp.AirTransportTcpServer;
import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import jpa.PersistenceSettings;

public final class AirtransportApp {

    private AirtransportApp() {}

    public static void main(final String[] args) {
        if (args.length >= 2 && "--server".equals(args[0])) {
            int port = 9002;
            try { port = Integer.parseInt(args[1]); } catch (final NumberFormatException ignored) {}
            AirTransportTcpServer.start(port);
            return;
        }

        if (args.length >= 3 && "--remote".equals(args[0])) {
            final String host = args[1];
            int port = 9002;
            try { port = Integer.parseInt(args[2]); } catch (final NumberFormatException ignored) {}
            System.out.println("=== AISafe Air Transport Company Console (US078 Remote) ===");
            System.out.println("Connecting to " + host + ":" + port + " ...");
            if (new TcpLoginUI(host, port).show()) {
                new MainMenu(true).mainLoop();
            } else {
                System.out.println("Login failed. Exiting.");
            }
            return;
        }

        // Local mode
        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));
        System.out.println("=== AISafe Air Transport Company Console ===");
        if (new LoginUI().show()) {
            new MainMenu().mainLoop();
        }
    }
}
