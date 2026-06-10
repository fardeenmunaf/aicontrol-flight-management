package javafxui.presentation.flightplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.SegmentAst;
import flight.dsl.model.FlightDslProcessingResult;
import flight.dsl.processing.FlightDslProcessor;
import flightPlan.application.ImportFlightPlanController;
import flightPlan.application.LegImportRequest;
import flightPlan.application.SegmentImportRequest;
import flightPlan.domain.FlightPlan;
import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import flightRoute.domain.FlightRoute;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import jpa.JpaPilotCompanyAssignmentRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class FlightPlanUI {

    // ── Table ──────────────────────────────────────────────────────────────────
    @FXML
    private TableView<FlightPlanRow> planTable;
    @FXML
    private TableColumn<FlightPlanRow, String> colId;
    @FXML
    private TableColumn<FlightPlanRow, String> colAircraft;
    @FXML
    private TableColumn<FlightPlanRow, String> colStatus;
    @FXML
    private TableColumn<FlightPlanRow, String> colDeparture;
    @FXML
    private Label tableStatus;

    // ── Form header ────────────────────────────────────────────────────────────
    @FXML
    private TextField fDesignator;
    @FXML
    private ComboBox<String> fType;
    @FXML
    private ComboBox<Aircraft> fAircraft;
    @FXML
    private ComboBox<FlightRoute> fRouteRef;
    @FXML
    private TextField fOrigin;
    @FXML
    private TextField fDestination;

    // ── Pilot warning banner ───────────────────────────────────────────────────
    @FXML private Label  pilotWarning;
    @FXML private Button btnNewFlightPlan;
    @FXML private Button btnImportFile;

    // ── Right panel switching ──────────────────────────────────────────────────
    @FXML private VBox  detailPanel;
    @FXML private Label detailTitle;
    @FXML private VBox  detailContent;
    @FXML private VBox  formPanel;

    // ── Dynamic legs ───────────────────────────────────────────────────────────
    @FXML
    private VBox legsContainer;
    @FXML
    private Label formResult;

    private static final String FLIGHTPLANS_DIR = "aisafe-simulation/flightplans";
    private static final String[] SEGMENT_MODES = { "climb", "cruise", "descend" };
    private static final DateTimeFormatter DEPARTURE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final List<LegEntry> legs = new ArrayList<>();
    private final ObservableList<FlightPlanRow> data = FXCollections.observableArrayList();
    private final ObjectMapper mapper = new ObjectMapper();
    private final FlightDslProcessor processor = new FlightDslProcessor();
    private boolean canCreatePlans = true;
    private String companyIataCode = null;
    private AirTransportCompanyId companyId = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().designator()));
        colAircraft.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().aircraft()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        colDeparture.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().departure()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle("FILE".equals(item)
                        ? "-fx-text-fill:#42a5f5;"
                        : "-fx-text-fill:#e0e0e0;");
            }
        });

        planTable.getColumns().add(buildActionColumn());
        planTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        planTable.setItems(data);

        planTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                final FlightPlanRow row = planTable.getSelectionModel().getSelectedItem();
                if (row != null) showDetail(row);
            }
        });

        fType.setItems(FXCollections.observableArrayList("regular", "charter"));
        fType.setValue("regular");

        checkPilotEligibility();

        try {
            refreshTable();
        } catch (final Exception e) {
            tableStatus.setText("Could not load flight plans: " + e.getMessage());
        }
        addLegEntry();
    }

    private void checkPilotEligibility() {
        try {
            final String email = AuthzRegistry.authorizationService().session()
                    .map(s -> s.authenticatedUser().username().toString())
                    .orElse(null);
            if (email == null) return;

            final UserId userId = UserId.valueOf(email);
            final var assignment = new JpaPilotCompanyAssignmentRepository("aisafe-pu")
                    .ofIdentity(userId);

            if (assignment.isEmpty()) {
                showWarning("You must be associated with an Air Transport Company before creating flight plans. Please contact your company administrator.");
                disableForm();
                return;
            }

            companyId = assignment.get().companyId();
            PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(companyId)
                    .ifPresent(c -> companyIataCode = c.iataCode());

            final boolean isInactive = StreamSupport
                    .stream(PersistenceContext.repositories().pilots().findAll().spliterator(), false)
                    .filter(p -> email.equalsIgnoreCase(p.userId().toString()))
                    .map(Pilot::status)
                    .anyMatch(s -> s == PilotStatus.INACTIVE);

            if (isInactive) {
                showWarning("Your pilot account is inactive. You may view existing flight plans but cannot create new ones.");
                disableForm();
                return;
            }

            setupFormForCompany();
        } catch (final Exception ignored) {
        }
    }

    private void setupFormForCompany() {
        if (companyIataCode != null) {
            fDesignator.setText(companyIataCode);
            fDesignator.setPromptText(companyIataCode + "1234");
        }

        if (companyId != null) {
            final List<Aircraft> fleet = PersistenceContext.repositories().aircraft()
                    .findByAirTransportCompanyId(companyId).stream()
                    .filter(a -> a.operationStatus() != AircraftOperationStatus.DECOMISSIONED)
                    .toList();
            fAircraft.setItems(FXCollections.observableArrayList(fleet));
            fAircraft.setConverter(new StringConverter<>() {
                @Override
                public String toString(final Aircraft a) {
                    if (a == null) return "";
                    return String.format("%s  (%s)%s",
                            a.registrationNumber(),
                            a.aircraftModel().modelName(),
                            a.operationStatus() == AircraftOperationStatus.MAINTENANCE ? "  [maintenance]" : "");
                }
                @Override
                public Aircraft fromString(final String s) { return null; }
            });
        }

        final StringConverter<FlightRoute> routeConverter = new StringConverter<>() {
            @Override
            public String toString(final FlightRoute r) {
                if (r == null) return "";
                return String.format("%s  (%s → %s)%s",
                        r.designation().value(),
                        r.originAirport().iataCode(),
                        r.destinationAirport().iataCode(),
                        r.isActive() ? "" : "  [inactive]");
            }
            @Override
            public FlightRoute fromString(final String s) { return null; }
        };
        fRouteRef.setConverter(routeConverter);

        if (companyId != null) {
            final List<FlightRoute> routes = new ArrayList<>();
            StreamSupport.stream(
                    PersistenceContext.repositories().flightRoutes()
                            .findByCompanyId(companyId).spliterator(), false)
                    .forEach(routes::add);
            fRouteRef.setItems(FXCollections.observableArrayList(routes));
        }

        fRouteRef.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                fOrigin.setText(newVal.originAirport().iataCode());
                fDestination.setText(newVal.destinationAirport().iataCode());
                if (!legs.isEmpty()) {
                    legs.get(0).depAirport.setText(newVal.originAirport().iataCode());
                    legs.get(legs.size() - 1).arrAirport.setText(newVal.destinationAirport().iataCode());
                }
            }
        });
    }

    private void showWarning(final String message) {
        pilotWarning.setText(message);
        pilotWarning.setVisible(true);
        pilotWarning.setManaged(true);
    }

    private void disableForm() {
        canCreatePlans = false;
        btnNewFlightPlan.setVisible(false);
        btnNewFlightPlan.setManaged(false);
        btnImportFile.setVisible(false);
        btnImportFile.setManaged(false);
        formPanel.setVisible(false);
        formPanel.setManaged(false);
    }

    // ── Right-panel switching ──────────────────────────────────────────────────

    @FXML private void onShowNewForm() {
        if (!canCreatePlans) return;
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        formPanel.setVisible(true);
        formPanel.setManaged(true);
        planTable.getSelectionModel().clearSelection();
    }

    @FXML private void onImportFromFile() {
        if (!canCreatePlans) return;
        final Window window = planTable.getScene().getWindow();
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Flight Plan DSL File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Flight DSL files", "*.dsl", "*.flight", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        final File selected = chooser.showOpenDialog(window);
        if (selected == null) return;

        try {
            final FlightDslProcessingResult result = processor.process(selected.toPath());
            if (!result.isValid()) {
                final StringBuilder sb = new StringBuilder("DSL errors in file:\n");
                result.semanticErrors().forEach(e -> sb.append("  • ").append(e).append("\n"));
                showFileImportResult(sb.toString(), false);
                return;
            }

            final ImportFlightPlanController importCtrl = new ImportFlightPlanController(
                    PersistenceContext.repositories().flightPlans(),
                    PersistenceContext.repositories().nodes(),
                    PersistenceContext.repositories().aircraftModels(),
                    PersistenceContext.repositories().airports());

            int imported = 0;
            final StringBuilder sb = new StringBuilder();
            for (final FlightPlanAst planAst : result.document().flightPlans()) {
                try {
                    importCtrl.importFlightPlan(
                            planAst.aircraftId(), planAst.pilotRef(), planAst.routeReference(), buildLegRequests(planAst));
                    sb.append("✓ Imported: ").append(planAst.flightDesignator()).append("\n");
                    imported++;
                } catch (final Exception e) {
                    sb.append("✗ ").append(planAst.flightDesignator())
                            .append(": ").append(e.getMessage()).append("\n");
                }
            }
            if (imported > 0) refreshTable();
            showFileImportResult(sb.toString().trim(), imported > 0);
        } catch (final Exception e) {
            showFileImportResult("Parse error: " + e.getMessage(), false);
        }
    }

    private void showFileImportResult(final String message, final boolean success) {
        final Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle("DSL File Import");
        alert.setHeaderText(success ? "Import successful" : "Import failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML private void onCloseDetail() {
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        if (canCreatePlans) {
            formPanel.setVisible(true);
            formPanel.setManaged(true);
        }
        planTable.getSelectionModel().clearSelection();
    }

    private void showDetail(final FlightPlanRow row) {
        detailTitle.setText(row.designator());
        detailContent.getChildren().clear();
        if (row.sourceFile() != null) {
            buildFileDetail(row);
        } else {
            buildDbDetail(row);
        }
        formPanel.setVisible(false);
        formPanel.setManaged(false);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
    }

    private void buildFileDetail(final FlightPlanRow row) {
        try {
            final JsonNode root = mapper.readTree(row.sourceFile().toFile());
            final JsonNode plan = root.isArray() ? root.get(0) : root;
            addDetailSection("Flight Information");
            addDetailField("Designator", plan.path("flightDesignator").asText());
            addDetailField("Type", plan.path("type").asText());
            addDetailField("Aircraft", plan.path("aircraftId").asText());
            final JsonNode route = plan.path("route");
            addDetailField("Route", route.path("reference").asText()
                    + "  " + route.path("origin").asText()
                    + " → " + route.path("destination").asText());

            final JsonNode legs = plan.path("legs");
            if (legs.isArray()) {
                int n = 1;
                for (final JsonNode leg : legs) {
                    addDetailSection("Leg " + n++);
                    final JsonNode dep = leg.path("departure");
                    addDetailField("Departure", dep.path("airport").asText()
                            + "  " + dep.path("date").asText()
                            + " " + dep.path("time").asText());
                    final JsonNode arr = leg.path("arrival");
                    addDetailField("Arrival", arr.path("airport").asText()
                            + "  " + arr.path("date").asText()
                            + " " + arr.path("time").asText());
                    final JsonNode fuel = leg.path("fuel");
                    addDetailField("Fuel", fuel.path("quantity").asText()
                            + " " + fuel.path("unit").asText());
                    final JsonNode segs = leg.path("segments");
                    if (segs.isArray() && segs.size() > 0) {
                        int sn = 1;
                        for (final JsonNode seg : segs) {
                            addDetailField("Segment " + sn++ + " mode", seg.path("mode").asText());
                            final JsonNode from = seg.path("from");
                            addDetailField("  From", from.path("latitude").asText()
                                    + ", " + from.path("longitude").asText()
                                    + "  alt " + from.path("altitude").asText()
                                    + " " + from.path("altitudeUnit").asText());
                            final JsonNode to = seg.path("to");
                            addDetailField("  To", to.path("latitude").asText()
                                    + ", " + to.path("longitude").asText()
                                    + "  alt " + to.path("altitude").asText()
                                    + " " + to.path("altitudeUnit").asText());
                        }
                    }
                }
            }
        } catch (final IOException e) {
            addDetailField("Error", "Could not parse file: " + e.getMessage());
        }
    }

    private void buildDbDetail(final FlightPlanRow row) {
        final FlightPlan fp = row.dbPlan();
        addDetailSection("Flight Information");
        addDetailField("ID", fp.identity().toString());
        addDetailField("Status", fp.status().name());
        try {
            final JsonNode plan = mapper.readTree(fp.flightPlanJson());
            addDetailField("Aircraft", plan.path("aircraftId").asText("?"));
            addDetailField("Pilot", plan.path("pilotRef").asText("?"));
            addDetailField("Route", plan.path("routeReference").asText("?"));
            final JsonNode legs = plan.path("legs");
            if (legs.isArray()) {
                int n = 1;
                for (final JsonNode leg : legs) {
                    addDetailSection("Leg " + n++);
                    final JsonNode dep = leg.path("departure");
                    addDetailField("Departure", dep.path("airport").asText()
                            + "  " + dep.path("date").asText()
                            + " " + dep.path("time").asText());
                    final JsonNode arr = leg.path("arrival");
                    addDetailField("Arrival", arr.path("airport").asText()
                            + "  " + arr.path("date").asText()
                            + " " + arr.path("time").asText());
                    final JsonNode fuel = leg.path("fuel");
                    addDetailField("Fuel", fuel.path("quantity").asText()
                            + " " + fuel.path("unit").asText());
                }
            }
        } catch (final IOException e) {
            addDetailField("Error", "Could not parse JSON: " + e.getMessage());
        }
    }

    private void addDetailSection(final String title) {
        final Label l = new Label(title);
        l.setStyle("-fx-text-fill:#42a5f5;-fx-font-weight:bold;-fx-padding:8 0 2 0;");
        detailContent.getChildren().add(l);
    }

    private void addDetailField(final String label, final String value) {
        final HBox row = new HBox(8);
        final Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-text-fill:#90caf9;-fx-min-width:120;-fx-font-size:11px;");
        final Label val = new Label(value);
        val.setStyle("-fx-text-fill:#e0e0e0;");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        detailContent.getChildren().add(row);
    }

    private TableColumn<FlightPlanRow, Void> buildActionColumn() {
        final TableColumn<FlightPlanRow, Void> col = new TableColumn<>("Action");
        col.setPrefWidth(80);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.getStyleClass().add("btn-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                deleteBtn.setOnAction(e -> onDeleteRow(getTableView().getItems().get(getIndex())));
                setGraphic(deleteBtn);
            }
        });
        return col;
    }

    private void onDeleteRow(final FlightPlanRow row) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete flight plan \"" + row.designator() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                if (row.sourceFile() != null) {
                    Files.deleteIfExists(row.sourceFile());
                } else if (row.dbPlan() != null) {
                    PersistenceContext.repositories().flightPlans().delete(row.dbPlan());
                }
                refreshTable();
            } catch (final Exception e) {
                tableStatus.setText("Error: " + e.getMessage());
            }
        });
    }

    private void refreshTable() {
        final List<FlightPlanRow> rows = new ArrayList<>();

        // Load from DB
        PersistenceContext.repositories().flightPlans().findAll()
                .forEach(fp -> rows.add(FlightPlanRow.fromDb(fp, DEPARTURE_FMT)));

        // Load from JSON files in flightplans directory
        final Path dir = Paths.get(FLIGHTPLANS_DIR);
        if (Files.isDirectory(dir)) {
            final File[] files = dir.toFile().listFiles(
                    f -> f.isFile() && f.getName().endsWith(".json"));
            if (files != null) {
                for (final File file : files) {
                    rows.addAll(readJsonFile(file));
                }
            }
        }

        data.setAll(rows);
        tableStatus.setText(rows.size() + " flight plans");
    }

    private List<FlightPlanRow> readJsonFile(final File file) {
        final List<FlightPlanRow> rows = new ArrayList<>();
        try {
            final JsonNode root = mapper.readTree(file);
            final JsonNode array = root.isArray() ? root : mapper.createArrayNode().add(root);
            for (final JsonNode plan : array) {
                final String designator = plan.path("flightDesignator").asText("?");
                final String aircraft = plan.path("aircraftId").asText("?");
                final String departure = firstLegDeparture(plan);
                rows.add(FlightPlanRow.fromFile(designator, aircraft, departure, file.toPath()));
            }
        } catch (final IOException e) {
            rows.add(FlightPlanRow.fromFile("(parse error: " + file.getName() + ")", "", "", file.toPath()));
        }
        return rows;
    }

    private String firstLegDeparture(final JsonNode plan) {
        final JsonNode legs = plan.path("legs");
        if (legs.isArray() && legs.size() > 0) {
            final JsonNode dep = legs.get(0).path("departure");
            final String date = dep.path("date").asText("");
            final String time = dep.path("time").asText("");
            return date.isEmpty() ? "" : date + " " + time;
        }
        return "";
    }

    // ── Leg management ─────────────────────────────────────────────────────────

    @FXML
    private void onAddLeg() {
        addLegEntry();
    }

    private void addLegEntry() {
        final int num = legs.size() + 1;
        final LegEntry entry = new LegEntry(num);
        legs.add(entry);
        legsContainer.getChildren().add(entry.panel);

        final FlightRoute route = fRouteRef == null ? null : fRouteRef.getValue();
        if (route != null) {
            if (num == 1) {
                entry.depAirport.setText(route.originAirport().iataCode());
            }
            entry.arrAirport.setText(route.destinationAirport().iataCode());
            // clear arrAirport from the previous last leg (it's now an intermediate)
            if (legs.size() > 1) {
                legs.get(legs.size() - 2).arrAirport.clear();
            }
        }
    }

    // ── Form actions ───────────────────────────────────────────────────────────

    @FXML
    private void onClearForm() {
        fDesignator.setText(companyIataCode != null ? companyIataCode : "");
        fAircraft.setValue(null);
        fRouteRef.setValue(null);
        fOrigin.clear();
        fDestination.clear();
        fType.setValue("regular");
        legs.clear();
        legsContainer.getChildren().clear();
        formResult.setText("");
        addLegEntry();
    }

    @FXML
    private void onGenerateImport() {
        try {
            final String dsl = buildDsl();
            if (dsl == null)
                return;

            final FlightDslProcessingResult result = processor.process(dsl);
            if (!result.isValid()) {
                final StringBuilder sb = new StringBuilder("Errors:\n");
                result.semanticErrors().forEach(e -> sb.append("  • ").append(e).append("\n"));
                formResult.setText(sb.toString());
                return;
            }

            final ImportFlightPlanController importCtrl = new ImportFlightPlanController(
                    PersistenceContext.repositories().flightPlans(),
                    PersistenceContext.repositories().nodes(),
                    PersistenceContext.repositories().aircraftModels(),
                    PersistenceContext.repositories().airports());

            int imported = 0;
            final StringBuilder sb = new StringBuilder();
            for (final FlightPlanAst planAst : result.document().flightPlans()) {
                try {
                    importCtrl.importFlightPlan(
                            planAst.aircraftId(), planAst.pilotRef(), planAst.routeReference(), buildLegRequests(planAst));
                    sb.append("✓ Imported: ").append(planAst.flightDesignator()).append("\n");
                    imported++;
                } catch (final Exception e) {
                    sb.append("✗ ").append(planAst.flightDesignator())
                            .append(": ").append(e.getMessage()).append("\n");
                }
            }
            if (imported > 0) {
                refreshTable();
                onClearForm();
            }
            formResult.setText(sb.toString().trim());
        } catch (final IllegalArgumentException e) {
            formResult.setText("Validation error: " + e.getMessage());
        } catch (final Exception e) {
            formResult.setText("Error: " + e.getMessage());
        }
    }

    // ── DSL builder ────────────────────────────────────────────────────────────

    private String buildDsl() {
        final String designator = require(fDesignator, "Flight Designator");
        final String type = fType.getValue() == null ? "regular" : fType.getValue();
        final Aircraft selectedAircraft = fAircraft.getValue();
        if (selectedAircraft == null) throw new IllegalArgumentException("Aircraft is required. Please select an aircraft from the fleet.");
        final String aircraft = selectedAircraft.aircraftModel().modelName();

        final FlightRoute selectedRoute = fRouteRef.getValue();
        if (selectedRoute == null) throw new IllegalArgumentException("Route is required. Please select a route from the list.");
        final String routeRef = selectedRoute.designation().value();
        final String origin = fOrigin.getText().trim().isEmpty()
                ? selectedRoute.originAirport().iataCode() : fOrigin.getText().trim();
        final String dest = fDestination.getText().trim().isEmpty()
                ? selectedRoute.destinationAirport().iataCode() : fDestination.getText().trim();

        if (legs.isEmpty()) {
            formResult.setText("At least one leg is required.");
            return null;
        }

        final String pilotEmail = AuthzRegistry.authorizationService().session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated session"));

        final StringBuilder sb = new StringBuilder();
        sb.append("flight ").append(designator).append(" {\n");
        sb.append("    type:     ").append(type).append(";\n");
        sb.append("    aircraft: ").append(aircraft).append(";\n");
        sb.append("    pilot:    ").append(pilotEmail).append(";\n");
        sb.append("    route:    ").append(routeRef)
                .append(" from ").append(origin).append(" to ").append(dest).append(";\n\n");

        for (int i = 0; i < legs.size(); i++) {
            final LegEntry leg = legs.get(i);
            final String depAirport = require(leg.depAirport, "Leg " + (i + 1) + " Departure Airport");
            final String depDate = require(leg.depDate, "Leg " + (i + 1) + " Departure Date");
            final String depTime = require(leg.depTime, "Leg " + (i + 1) + " Departure Time");
            final String arrAirport = require(leg.arrAirport, "Leg " + (i + 1) + " Arrival Airport");
            final String arrDate = require(leg.arrDate, "Leg " + (i + 1) + " Arrival Date");
            final String arrTime = require(leg.arrTime, "Leg " + (i + 1) + " Arrival Time");
            final String fuelQty = require(leg.fuelQty, "Leg " + (i + 1) + " Fuel Quantity");
            final String fuelUnit = leg.fuelUnit.getValue() == null ? "kg" : leg.fuelUnit.getValue();

            sb.append("    leg {\n");
            sb.append("        departure {\n");
            sb.append("            airport: ").append(depAirport).append(";\n");
            sb.append("            date:    ").append(depDate).append(";\n");
            sb.append("            time:    ").append(depTime).append(";\n");
            sb.append("        }\n");
            sb.append("        arrival {\n");
            sb.append("            airport: ").append(arrAirport).append(";\n");
            sb.append("            date:    ").append(arrDate).append(";\n");
            sb.append("            time:    ").append(arrTime).append(";\n");
            sb.append("        }\n");
            sb.append("        fuel {\n");
            sb.append("            quantity: ").append(fuelQty).append(" ").append(fuelUnit).append(";\n");
            sb.append("        }\n");

            for (int s = 0; s < SEGMENT_MODES.length; s++) {
                final String fLat = require(leg.fromLat[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " From Lat");
                final String fLon = require(leg.fromLon[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " From Lon");
                final String fAlt = require(leg.fromAlt[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " From Alt");
                final String fUnit = leg.fromAltUnit[s].getValue() == null ? "m" : leg.fromAltUnit[s].getValue();
                final String tLat = require(leg.toLat[s], "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " To Lat");
                final String tLon = require(leg.toLon[s], "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " To Lon");
                final String tAlt = require(leg.toAlt[s], "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " To Alt");
                final String tUnit = leg.toAltUnit[s].getValue() == null ? "m" : leg.toAltUnit[s].getValue();
                final String sMin = require(leg.slotMin[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " Slot Min");
                final String sMinU = leg.slotMinUnit[s].getValue() == null ? "m" : leg.slotMinUnit[s].getValue();
                final String sMax = require(leg.slotMax[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " Slot Max");
                final String sMaxU = leg.slotMaxUnit[s].getValue() == null ? "m" : leg.slotMaxUnit[s].getValue();
                final String sWidth = require(leg.slotWidth[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " Slot Width");
                final String sWidthU = leg.slotWidthUnit[s].getValue() == null ? "m" : leg.slotWidthUnit[s].getValue();
                final String wDir = require(leg.windDir[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " Wind Direction");
                final String wSpeed = require(leg.windSpeed[s],
                        "Leg " + (i + 1) + " " + SEGMENT_MODES[s] + " Wind Speed");
                sb.append("        segment {\n");
                sb.append("            from: (").append(fLat).append(", ").append(fLon)
                        .append(", altitude: ").append(fAlt).append(" ").append(fUnit).append(");\n");
                sb.append("            to:   (").append(tLat).append(", ").append(tLon)
                        .append(", altitude: ").append(tAlt).append(" ").append(tUnit).append(");\n");
                sb.append("            altslot[min: ").append(sMin).append(" ").append(sMinU)
                        .append(", max: ").append(sMax).append(" ").append(sMaxU)
                        .append(", width: ").append(sWidth).append(" ").append(sWidthU).append("];\n");
                sb.append("            wind {\n");
                sb.append("                direction: ").append(wDir).append(";\n");
                sb.append("                speed: ").append(wSpeed).append(" m/s;\n");
                sb.append("            }\n");
                sb.append("            mode: ").append(SEGMENT_MODES[s]).append(";\n");
                sb.append("        }\n");
            }
            sb.append("    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String require(final TextField f, final String name) {
        final String v = f.getText().trim();
        if (v.isEmpty())
            throw new IllegalArgumentException(name + " is required.");
        return v;
    }

    private List<LegImportRequest> buildLegRequests(final FlightPlanAst planAst) {
        final List<LegImportRequest> legRequests = new ArrayList<>();
        int seq = 1;
        for (final LegAst legAst : planAst.legs()) {
            final List<SegmentImportRequest> segRequests = new ArrayList<>();
            for (final SegmentAst segAst : legAst.segments()) {
                segRequests.add(new SegmentImportRequest(
                        segAst.from().latitude(), segAst.from().longitude(),
                        segAst.to().latitude(), segAst.to().longitude()));
            }
            legRequests.add(new LegImportRequest(seq++,
                    legAst.departure().date(), legAst.departure().time(),
                    legAst.arrival().date(), legAst.arrival().time(),
                    legAst.fuel().quantity(),
                    legAst.departure().airportCode(), legAst.arrival().airportCode(),
                    segRequests));
        }
        return legRequests;
    }

    // ── FlightPlanRow: unified display model for table ─────────────────────────

    public static final class FlightPlanRow {
        private final String designator;
        private final String aircraft;
        private final String status;
        private final String departure;
        private final Path sourceFile; // non-null for file-backed rows
        private final FlightPlan dbPlan; // non-null for DB-backed rows

        private FlightPlanRow(final String designator, final String aircraft,
                final String status, final String departure,
                final Path sourceFile, final FlightPlan dbPlan) {
            this.designator = designator;
            this.aircraft = aircraft;
            this.status = status;
            this.departure = departure;
            this.sourceFile = sourceFile;
            this.dbPlan = dbPlan;
        }

        static FlightPlanRow fromDb(final FlightPlan fp, final DateTimeFormatter fmt) {
            final String aircraft = fp.aircraftId() != null ? fp.aircraftId() : "?";
            final String departure = fp.departureTime() != null ? fmt.format(fp.departureTime()) : "?";
            return new FlightPlanRow(fp.identity().toString(), aircraft, fp.status().name(), departure, null, fp);
        }

        static FlightPlanRow fromFile(final String designator, final String aircraft,
                final String departure, final Path file) {
            return new FlightPlanRow(designator, aircraft, "FILE", departure, file, null);
        }

        public String designator() {
            return designator;
        }

        public String aircraft() {
            return aircraft;
        }

        public String status() {
            return status;
        }

        public String departure() {
            return departure;
        }

        public Path sourceFile() {
            return sourceFile;
        }

        public FlightPlan dbPlan() {
            return dbPlan;
        }
    }

    // ── LegEntry: dynamic form section for one leg ─────────────────────────────

    private class LegEntry {
        final VBox panel;
        final TextField depAirport, depDate, depTime;
        final TextField arrAirport, arrDate, arrTime;
        final TextField fuelQty;
        final ComboBox<String> fuelUnit;

        final TextField[] fromLat = new TextField[3];
        final TextField[] fromLon = new TextField[3];
        final TextField[] fromAlt = new TextField[3];
        @SuppressWarnings("unchecked")
        final ComboBox<String>[] fromAltUnit = new ComboBox[3];
        final TextField[] toLat = new TextField[3];
        final TextField[] toLon = new TextField[3];
        final TextField[] toAlt = new TextField[3];
        @SuppressWarnings("unchecked")
        final ComboBox<String>[] toAltUnit = new ComboBox[3];
        final TextField[] slotMin = new TextField[3];
        @SuppressWarnings("unchecked")
        final ComboBox<String>[] slotMinUnit = new ComboBox[3];
        final TextField[] slotMax = new TextField[3];
        @SuppressWarnings("unchecked")
        final ComboBox<String>[] slotMaxUnit = new ComboBox[3];
        final TextField[] slotWidth = new TextField[3];
        @SuppressWarnings("unchecked")
        final ComboBox<String>[] slotWidthUnit = new ComboBox[3];
        final TextField[] windDir = new TextField[3];
        final TextField[] windSpeed = new TextField[3];

        LegEntry(final int num) {
            panel = new VBox(8);
            panel.setStyle("-fx-background-color:#0f3460;-fx-padding:12;-fx-border-radius:4;-fx-background-radius:4;");

            final Label title = new Label("Leg " + num);
            title.setStyle("-fx-text-fill:#42a5f5;-fx-font-weight:bold;");

            final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            final LocalDateTime defaultDep = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
            final LocalDateTime defaultArr = defaultDep.plusHours(2);

            final Label depTitle = sectionLabel("Departure");
            depAirport = field("Airport (e.g. OPO)");
            depDate = fieldWithDefault("yyyy-MM-dd", defaultDep.format(dateFmt));
            depTime = fieldWithDefault("HH:mm", defaultDep.format(timeFmt));
            final HBox depRow = labeled3("Airport", depAirport, "Date", depDate, "Time", depTime);

            final Label arrTitle = sectionLabel("Arrival");
            arrAirport = field("Airport (e.g. MAD)");
            arrDate = fieldWithDefault("yyyy-MM-dd", defaultArr.format(dateFmt));
            arrTime = fieldWithDefault("HH:mm", defaultArr.format(timeFmt));
            final HBox arrRow = labeled3("Airport", arrAirport, "Date", arrDate, "Time", arrTime);

            final Label fuelTitle = sectionLabel("Fuel");
            fuelQty = field("Quantity (e.g. 40200)");
            fuelUnit = new ComboBox<>(FXCollections.observableArrayList("kg", "l"));
            fuelUnit.setValue("kg");
            fuelUnit.setMaxWidth(80);
            final HBox fuelRow = new HBox(12, labeled("Quantity", fuelQty), labeled("Unit", fuelUnit));

            final Label segsTitle = sectionLabel("Flight Segments (climb · cruise · descend)");
            final VBox segmentsPanel = new VBox(8);

            for (int s = 0; s < 3; s++) {
                fromLat[s] = field("Lat (e.g. 41.262891)");
                fromLon[s] = field("Lon (e.g. -8.68522)");
                fromAlt[s] = field("Alt (e.g. 69)");
                fromAltUnit[s] = altUnitBox();
                toLat[s] = field("Lat (e.g. 42.0)");
                toLon[s] = field("Lon (e.g. -8.01)");
                toAlt[s] = field("Alt (e.g. 9249)");
                toAltUnit[s] = altUnitBox();

                slotMin[s] = fieldWithDefault("Min alt", "0");
                slotMinUnit[s] = altUnitBox();
                slotMax[s] = fieldWithDefault("Max alt", "10000");
                slotMaxUnit[s] = altUnitBox();
                slotWidth[s] = fieldWithDefault("Width", "500");
                slotWidthUnit[s] = altUnitBox();

                windDir[s] = fieldWithDefault("0–359", "180");
                windSpeed[s] = fieldWithDefault("m/s", "5");

                final Label segTitle = sectionLabel(capitalize(SEGMENT_MODES[s]) + " Segment");
                final HBox fromRow = new HBox(8,
                        labeled("From Lat", fromLat[s]), labeled("Lon", fromLon[s]),
                        labeled("Alt", fromAlt[s]), labeled("Unit", fromAltUnit[s]));
                final HBox toRow = new HBox(8,
                        labeled("To Lat", toLat[s]), labeled("Lon", toLon[s]),
                        labeled("Alt", toAlt[s]), labeled("Unit", toAltUnit[s]));
                final HBox slotRow = new HBox(8,
                        labeled("Slot Min", slotMin[s]), labeled("Unit", slotMinUnit[s]),
                        labeled("Max", slotMax[s]), labeled("Unit", slotMaxUnit[s]),
                        labeled("Width", slotWidth[s]), labeled("Unit", slotWidthUnit[s]));
                final HBox windRow = new HBox(8,
                        labeled("Wind Dir (°)", windDir[s]),
                        labeled("Speed (m/s)", windSpeed[s]));
                setHGrow(fromRow);
                setHGrow(toRow);
                setHGrow(slotRow);
                setHGrow(windRow);
                segmentsPanel.getChildren().addAll(segTitle, fromRow, toRow, slotRow, windRow);
            }

            if (num > 1) {
                final Button removeBtn = new Button("Remove Leg");
                removeBtn.getStyleClass().add("btn-danger");
                removeBtn.setOnAction(e -> {
                    legsContainer.getChildren().remove(panel);
                    legs.remove(this);
                });
                panel.getChildren().addAll(title, depTitle, depRow, arrTitle, arrRow,
                        fuelTitle, fuelRow, segsTitle, segmentsPanel, removeBtn);
            } else {
                panel.getChildren().addAll(title, depTitle, depRow, arrTitle, arrRow,
                        fuelTitle, fuelRow, segsTitle, segmentsPanel);
            }
        }

        private TextField field(final String prompt) {
            final TextField tf = new TextField();
            tf.setPromptText(prompt);
            return tf;
        }

        private TextField fieldWithDefault(final String prompt, final String defaultValue) {
            final TextField tf = new TextField(defaultValue);
            tf.setPromptText(prompt);
            return tf;
        }

        private ComboBox<String> altUnitBox() {
            final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("m", "ft"));
            cb.setValue("m");
            cb.setMaxWidth(60);
            return cb;
        }

        private Label sectionLabel(final String text) {
            final Label l = new Label(text);
            l.setStyle("-fx-text-fill:#90caf9;-fx-font-size:11px;-fx-padding:4 0 0 0;");
            return l;
        }

        private VBox labeled(final String lbl, final javafx.scene.Node ctrl) {
            final Label l = new Label(lbl);
            l.setStyle("-fx-text-fill:#b0bec5;-fx-font-size:10px;");
            final VBox vb = new VBox(2, l, ctrl);
            HBox.setHgrow(vb, Priority.ALWAYS);
            if (ctrl instanceof TextField) {
                ((TextField) ctrl).setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(ctrl, Priority.ALWAYS);
            }
            return vb;
        }

        private HBox labeled3(final String l1, final TextField f1,
                final String l2, final TextField f2,
                final String l3, final TextField f3) {
            final HBox hb = new HBox(8, labeled(l1, f1), labeled(l2, f2), labeled(l3, f3));
            setHGrow(hb);
            return hb;
        }

        private void setHGrow(final HBox hb) {
            hb.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        }

        private String capitalize(final String s) {
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
