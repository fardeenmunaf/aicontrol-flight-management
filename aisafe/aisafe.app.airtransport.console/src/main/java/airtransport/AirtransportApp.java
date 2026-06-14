package airtransport;

import airtransport.presentation.MainMenu;
import airtransport.presentation.authz.LoginUI;
import airtransport.presentation.authz.TcpLoginUI;
import airtransport.tcp.AirTransportTcpServer;
import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import infrastructure.persistence.PersistenceContext;
import jpa.PersistenceSettings;
import simulation.application.DbFlightPlanResolver;
import simulation.application.RecordSimulationResultService;
import simulation.application.SimulationResultParser;
import simulation.domain.Simulation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

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

        // Headless recorder mode: no login required.
        // Reads the newest simulation_report_*.json from aisafe-simulation/ and
        // persists it to the database.  Invoked by run-simulation-and-record.sh.
        if (args.length >= 1 && "--record".equals(args[0])) {
            // JPA requires AuthzConfigurator even for headless operation.
            AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                    PersistenceSettings.getPersistenceUnitName(),
                    PersistenceSettings.getExtendedPersistenceProperties()));
            headlessRecord();
            return;
        }

        // Local interactive mode
        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));
        System.out.println("=== AISafe Air Transport Company Console ===");
        if (new LoginUI().show()) {
            new MainMenu().mainLoop();
        }
    }

    /**
     * Finds the newest simulation_report_*.json produced by the C engine,
     * parses it and persists the result via {@link RecordSimulationResultService}.
     * No user authentication is required — this is a trusted local invocation.
     */
    private static void headlessRecord() {
        try {
            final Path simDir = Paths.get("aisafe-simulation").toAbsolutePath();
            final Path flightPlansDir = simDir.resolve("flightplansSim");

            // Find newest report file
            final Path report = Files.list(simDir)
                    .filter(p -> p.getFileName().toString().matches("simulation_report_.*\\.json"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .orElseThrow(() -> new IllegalStateException(
                            "No simulation_report_*.json found in " + simDir));

            System.out.println("Recording report: " + report.getFileName());

            final DbFlightPlanResolver resolver = new DbFlightPlanResolver(
                    PersistenceContext.repositories().flightPlans(),
                    PersistenceContext.repositories().airports());

            final RecordSimulationResultService recorder = new RecordSimulationResultService(
                    PersistenceContext.repositories().simulations(),
                    resolver,
                    resolver);

            final Simulation sim = recorder.record(
                    new SimulationResultParser().parse(Files.readString(report)));

            System.out.println("Simulation recorded successfully.");
            System.out.println("  Flights  : " + sim.simulationFlights().size());
            System.out.println("  Violations: " + sim.safetyViolations().size());

        } catch (final Exception e) {
            System.err.println("ERROR recording simulation: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
