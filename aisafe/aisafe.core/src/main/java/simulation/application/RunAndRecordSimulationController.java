package simulation.application;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import simulation.domain.Simulation;

public class RunAndRecordSimulationController {

    private final SimulationLauncher launcher;
    private final SimulationResultParser parser;
    private final RecordSimulationResultService recorder;

    public RunAndRecordSimulationController(final SimulationLauncher launcher,
            final SimulationResultParser parser, final RecordSimulationResultService recorder) {
        this.launcher = launcher;
        this.parser = parser;
        this.recorder = recorder;
    }

    /** Authorized entry point used by the UIs. */
    public Simulation runAndRecord() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return runAndRecordNoAuth();
    }

    /** Orchestration without the authz gate (for unit testing). */
    Simulation runAndRecordNoAuth() {
        return recorder.record(parser.parse(launcher.runAndReadReport()));
    }
}
