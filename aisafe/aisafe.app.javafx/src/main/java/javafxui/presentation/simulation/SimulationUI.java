package javafxui.presentation.simulation;

import airport.application.ListAirportsController;
import airport.domain.Airport;
import infrastructure.persistence.PersistenceContext;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class SimulationUI {

    @FXML private WebView mapView;
    @FXML private Label stepLabel;
    @FXML private Label activeLabel;
    @FXML private Label violationLabel;
    @FXML private Label violationText;
    @FXML private HBox  violationBanner;
    @FXML private Button runButton;

    private final ListAirportsController airportsController = new ListAirportsController(
            PersistenceContext.repositories().airports());
    private final SimulationStepParser    parser       = new SimulationStepParser();
    private final SimulationReportParser  reportParser = new SimulationReportParser();

    // Each entry is one simulation timestep worth of flight positions.
    private final Queue<List<SimStep>> stepBatches = new ConcurrentLinkedQueue<>();
    // Accumulates rows for the current timestep on the reader thread.
    private final List<SimStep> pendingBatch = new ArrayList<>();

    private volatile boolean simProcessDone = false;
    private volatile int    simExitCode     = 0;

    private int lastStep       = 0;
    private int lastActive     = 0;
    private int lastViolations = 0;

    private ScheduledExecutorService scheduler;
    private boolean mapReady = false;

    @FXML
    public void initialize() {
        final WebEngine engine = mapView.getEngine();
        final var mapResource = getClass().getResource("/javafxui/map.html");
        if (mapResource == null) {
            showBanner("Error: map.html not found");
            return;
        }
        engine.load(mapResource.toExternalForm());
        SimulationMapBridge.install(engine, iata -> {});
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    loadAirportPins(engine);
                    mapReady = true;
                });
            }
        });
    }

    private void loadAirportPins(final WebEngine engine) {
        for (final Airport airport : airportsController.listAirports()) {
            final String iata = airport.iataCode();
            final String name = airport.name().replace("'", "\\'");
            final double lat  = airport.coordinates().latitude();
            final double lon  = airport.coordinates().longitude();
            engine.executeScript(String.format(Locale.ROOT,
                    "addPin('%s','%s',%f,%f)", iata, name, lat, lon));
        }
    }

    @FXML
    private void onRunSimulation() {
        if (!mapReady) return;

        final List<Integer> selected = showFlightSelectionDialog();
        if (selected == null || selected.isEmpty()) return;

        runButton.setDisable(true);
        stepBatches.clear();
        pendingBatch.clear();
        simProcessDone = false;
        simExitCode    = 0;
        lastStep       = 0;
        lastActive     = 0;
        lastViolations = 0;
        mapView.getEngine().executeScript("clearFlights()");
        violationBanner.setVisible(false);
        violationBanner.setManaged(false);

        startStepScheduler();
        startSimProcess(selected);
    }

    /**
     * Lists flight plans from flightplansSim/ alphabetically (matching the C simulation order)
     * and shows a checkbox dialog. Returns the 1-based indices of selected flights, or null
     * if the user cancelled.
     */
    private List<Integer> showFlightSelectionDialog() {
        final File dir = Paths.get("aisafe-simulation/flightplansSim").toAbsolutePath().toFile();
        final File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            showBanner("No flight plans found in flightplansSim/");
            return null;
        }
        Arrays.sort(jsonFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        final List<CheckBox> checkBoxes = new ArrayList<>();
        final VBox box = new VBox(6);
        box.setPadding(new Insets(10));
        for (final File f : jsonFiles) {
            final String callsign = f.getName().replace(".json", "");
            final CheckBox cb = new CheckBox(callsign);
            checkBoxes.add(cb);
            box.getChildren().add(cb);
        }

        final Button selectAll = new Button("Select All");
        selectAll.setOnAction(e -> {
            final boolean allSelected = checkBoxes.stream().allMatch(CheckBox::isSelected);
            checkBoxes.forEach(cb -> cb.setSelected(!allSelected));
        });

        final VBox content = new VBox(8, selectAll, new ScrollPane(box));
        content.setPadding(new Insets(10));
        ((ScrollPane) content.getChildren().get(1)).setFitToWidth(true);
        ((ScrollPane) content.getChildren().get(1)).setPrefHeight(300);

        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Flight Selection");
        dialog.setHeaderText("Select flights to simulate:");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return null;

        final List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++)
            if (checkBoxes.get(i).isSelected())
                selected.add(i + 1);
        return selected;
    }

    private void startStepScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "sim-step-scheduler");
            t.setDaemon(true);
            return t;
        });
        // Each tick: apply one full simulation timestep (all flights at once).
        scheduler.scheduleAtFixedRate(() -> {
            final List<SimStep> batch = stepBatches.poll();
            if (batch != null) {
                Platform.runLater(() -> batch.forEach(this::applyStep));
            } else if (simProcessDone) {
                // Queue drained and process finished — wrap up.
                scheduler.shutdown();
                Platform.runLater(() -> finalizeSimulation(simExitCode));
            }
        }, 0, 1500, TimeUnit.MILLISECONDS);
    }

    private void applyStep(final SimStep step) {
        if (!step.phase().equals("GATE")) {
            mapView.getEngine().executeScript(String.format(Locale.ROOT,
                    "moveFlight('%s',%f,%f,'%s')",
                    step.callsign(), step.lat(), step.lon(), step.phase()));
        }
    }

    private void startSimProcess(final List<Integer> selectedFlights) {
        final Thread readerThread = new Thread(() -> {
            try {
                final File simDir = Paths.get("aisafe-simulation").toAbsolutePath().toFile();

                // Compile the simulation binary (make is a no-op if nothing changed).
                final Process make = new ProcessBuilder("make")
                        .directory(simDir)
                        .redirectErrorStream(true)
                        .start();
                if (make.waitFor() != 0) {
                    simProcessDone = true;
                    Platform.runLater(() -> showBanner("make failed — could not compile simulation"));
                    return;
                }

                final ProcessBuilder pb = new ProcessBuilder("./simulation_engine");
                pb.directory(simDir);
                pb.redirectErrorStream(true);
                final Process process = pb.start();

                // Write selected flight numbers followed by a blank line to confirm.
                try (final BufferedWriter stdin = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream()))) {
                    for (final int n : selectedFlights)
                        stdin.write(n + "\n");
                    stdin.write("\n");
                }

                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        processLine(line);
                    }
                }

                // Flush any remaining rows after the last banner.
                if (!pendingBatch.isEmpty()) {
                    stepBatches.add(new ArrayList<>(pendingBatch));
                    pendingBatch.clear();
                }

                simExitCode    = process.waitFor();
                simProcessDone = true;

            } catch (final Exception e) {
                simProcessDone = true;
                Platform.runLater(() -> {
                    if (scheduler != null) scheduler.shutdown();
                    showBanner("Simulation error: " + e.getMessage());
                    runButton.setDisable(false);
                });
            }
        }, "sim-process-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    // Called only from the reader thread.
    private void processLine(final String line) {
        parser.parseStepBanner(line).ifPresent(banner -> {
            // A new timestep starts — commit the previous batch.
            if (!pendingBatch.isEmpty()) {
                stepBatches.add(new ArrayList<>(pendingBatch));
                pendingBatch.clear();
            }
            lastStep       = banner.step();
            lastActive     = banner.active();
            lastViolations = banner.violations();
            Platform.runLater(() -> {
                stepLabel.setText("Step: " + banner.step());
                activeLabel.setText("Active: " + banner.active());
                violationLabel.setText("Violations: " + banner.violations());
            });
        });

        parser.parsePhysicsRow(line).ifPresent(pendingBatch::add);

        parser.parseViolation(line).ifPresent(callsign -> Platform.runLater(() -> {
            final String safe = callsign.replace("\\", "\\\\").replace("'", "\\'");
            mapView.getEngine().executeScript("flagViolation('" + safe + "')");
        }));
    }

    private void finalizeSimulation(final int exitCode) {
        runButton.setDisable(false);
        if (exitCode != 0) {
            showBanner("Simulation exited with code " + exitCode);
            return;
        }
        stepLabel.setText("Step: complete");
        showReportDialog();
    }

    private void showReportDialog() {
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format("%-22s %d%n",  "Steps completed:",  lastStep));
        sb.append(String.format("%-22s %d%n",  "Active at end:",    lastActive));
        sb.append(String.format("%-22s %d%n%n","Total violations:", lastViolations));

        reportParser.findAndParse().ifPresentOrElse(report -> {
            sb.append(String.format("%-22s %s%n",  "Validation result:", report.validationResult()));
            sb.append(String.format("%-22s %d%n%n","Total flights:",     report.totalFlights()));

            if (!report.violations().isEmpty()) {
                sb.append(String.format("%-4s  %-10s  %-10s  %-10s  %s%n",
                        "#", "Sim time", "Flight A", "Flight B", "Separation"));
                sb.append("─".repeat(54)).append("\n");
                for (final SimulationReportParser.ViolationEvent v : report.violations()) {
                    final long secs  = (long) v.timestamp();
                    final long h     = secs / 3600;
                    final long m     = (secs % 3600) / 60;
                    final long s     = secs % 60;
                    sb.append(String.format("%-4d  %02d:%02d:%02d    %-10s  %-10s  %.0f m%n",
                            v.id(), h, m, s, v.callsignA(), v.callsignB(), v.separationMeters()));
                }
            }
        }, () -> sb.append("(report file not found)"));

        final TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefWidth(520);
        area.setPrefHeight(320);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        final Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Simulation Report");
        dialog.setHeaderText("Simulation complete");
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.showAndWait();
    }

    private void showBanner(final String message) {
        violationText.setText(message);
        violationBanner.setVisible(true);
        violationBanner.setManaged(true);
    }
}
