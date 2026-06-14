package simulation.application;

/** Launches a simulation run and returns the raw report JSON it produced. */
public interface SimulationLauncher {
    String runAndReadReport();
}
