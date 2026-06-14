package simulation;

import simulation.application.SimulationLauncher;

import java.io.OutputStream;
import java.nio.file.*;
import java.util.Comparator;

/** Runs ./simulation_engine in the aisafe-simulation dir, auto-selecting all flights. */
public class ProcessSimulationLauncher implements SimulationLauncher {

    private final Path simDir;

    public ProcessSimulationLauncher(final Path simDir) {
        this.simDir = simDir;
    }

    @Override
    public String runAndReadReport() {
        try {
            final Process p = new ProcessBuilder("./simulation_engine")
                    .directory(simDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            try (OutputStream in = p.getOutputStream()) {
                in.write("a\n\n".getBytes());  // select all, then start
                in.flush();
            }
            p.getInputStream().readAllBytes();   // drain output so the process can finish
            p.waitFor();
            final Path newest = Files.list(simDir)
                    .filter(f -> f.getFileName().toString().matches("simulation_report_.*\\.json"))
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()))
                    .orElseThrow(() -> new IllegalStateException("No simulation_report_*.json produced"));
            return Files.readString(newest);
        } catch (final Exception e) {
            throw new IllegalStateException("Simulation run failed: " + e.getMessage(), e);
        }
    }
}
