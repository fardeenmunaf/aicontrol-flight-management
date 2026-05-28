package javafxui.presentation.aircraftmodel;

import aircraftmodel.application.AddEngineModelToAircraftController;
import aircraftmodel.application.RegisterAircraftModelController;
import aircraftmodel.application.RemoveEngineModelFromAircraftController;
import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.dto.AircraftModelDTO;
import auth.domain.AISafeRoles;
import enginemodel.domain.EngineModel;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AircraftModelsUI {

    @FXML
    private TableView<AircraftModelDTO> table;
    @FXML
    private TableColumn<AircraftModelDTO, String> colModel;
    @FXML
    private TableColumn<AircraftModelDTO, String> colMaker;
    @FXML
    private TableColumn<AircraftModelDTO, String> colType;
    @FXML
    private TableColumn<AircraftModelDTO, String> colEngines;
    @FXML
    private TableColumn<AircraftModelDTO, String> colCruise;
    @FXML
    private TableColumn<AircraftModelDTO, String> colAction;
    @FXML
    private Label statusLabel;

    // Register form
    @FXML
    private VBox registerForm;
    @FXML
    private TextField fModelName, fMakerName, fMakerCountry, fNumEngines;
    @FXML
    private TextField fEmptyWeight, fMaxTakeOff, fMaxZeroFuel, fMaxFuel;
    @FXML
    private TextField fServiceCeiling, fCruiseSpeed, fWingArea, fWingSpan;
    @FXML
    private TextField fDragCoef, fLiftCoef, fEngineNames;
    @FXML
    private ComboBox<AircraftType> fAircraftType;
    @FXML
    private Label registerError;

    // Add engine form
    @FXML
    private VBox addEngineForm;
    @FXML
    private ComboBox<AircraftModel> addAircraftBox;
    @FXML
    private ComboBox<EngineModel> addEngineBox;
    @FXML
    private Label addEngineError;

    // Remove engine form
    @FXML
    private VBox removeEngineForm;
    @FXML
    private ComboBox<AircraftModel> removeAircraftBox;
    @FXML
    private ComboBox<EngineModel> removeEngineBox;
    @FXML
    private Label removeEngineError;

    private final RegisterAircraftModelController registerCtrl = new RegisterAircraftModelController(
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().engineModels());
    private final AddEngineModelToAircraftController addCtrl = new AddEngineModelToAircraftController(
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().engineModels());
    private final RemoveEngineModelFromAircraftController removeCtrl = new RemoveEngineModelFromAircraftController(
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().engineModels(),
            PersistenceContext.repositories().aircraft());

    private final ObservableList<AircraftModelDTO> data = FXCollections.observableArrayList();

    private static final StringConverter<AircraftModel> AIRCRAFT_CONV = new StringConverter<>() {
        @Override
        public String toString(final AircraftModel m) {
            return m == null ? "" : m.modelName();
        }

        @Override
        public AircraftModel fromString(final String s) {
            return null;
        }
    };
    private static final StringConverter<EngineModel> ENGINE_CONV = new StringConverter<>() {
        @Override
        public String toString(final EngineModel m) {
            return m == null ? "" : m.name();
        }

        @Override
        public EngineModel fromString(final String s) {
            return null;
        }
    };

    @FXML
    public void initialize() {
        colModel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().modelName));
        colMaker.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().makerName + " (" + c.getValue().makerCountry + ")"));
        colType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().type != null ? c.getValue().type.name() : "—"));
        colEngines.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().numberOfEngines + "× " + String.join(", ", c.getValue().engineModelNames)));
        colCruise.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().cruiseSpeed != null ? String.format("%.0f m/s", c.getValue().cruiseSpeed) : "—"));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.getStyleClass().add("btn-danger");
                btn.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(data);

        fAircraftType.setItems(FXCollections.observableArrayList(AircraftType.values()));
        addAircraftBox.setConverter(AIRCRAFT_CONV);
        addEngineBox.setConverter(ENGINE_CONV);
        removeAircraftBox.setConverter(AIRCRAFT_CONV);
        removeEngineBox.setConverter(ENGINE_CONV);
        addAircraftBox.setOnAction(e -> refreshAddEngineBox());
        removeAircraftBox.setOnAction(e -> refreshRemoveEngineBox());

        refresh();
    }

    private void refresh() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        final List<AircraftModelDTO> list = new ArrayList<>();
        PersistenceContext.repositories().aircraftModels().findAll().forEach(m -> {
            final List<String> engineNames = m.supportedEngineModels().stream()
                    .map(EngineModel::name).collect(Collectors.toList());
            list.add(new AircraftModelDTO(m.modelName(), m.maker().name(), m.maker().country(),
                    m.type(), m.emptyWeight(), m.maximumTakeOffWeight(), m.maximumZeroFuelWeight(),
                    m.maximumFuelCapacity(), m.serviceCeiling(), m.cruiseSpeed(),
                    m.wingArea(), m.wingSpan(), m.dragCoefficient(), m.liftCoefficient(),
                    m.numberOfEngines(), engineNames));
        });
        data.setAll(list);
        statusLabel.setText(list.size() + " aircraft models");
    }

    private void onDelete(final AircraftModelDTO dto) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete aircraft model \"" + dto.modelName + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
                final var repo = PersistenceContext.repositories().aircraftModels();
                repo.findByModelNameAndMaker(dto.modelName, dto.makerName).stream()
                        .findFirst().ifPresent(repo::delete);
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @FXML
    private void onRegister() {
        hideAllForms();
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        registerError.setText("");
    }

    @FXML
    private void onCancelRegister() {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        clearRegisterForm();
    }

    @FXML
    private void onSaveRegister() {
        try {
            final List<String> engines = Arrays.stream(fEngineNames.getText().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            final AircraftModelDTO dto = new AircraftModelDTO(
                    fModelName.getText().trim(), fMakerName.getText().trim(),
                    fMakerCountry.getText().trim(), fAircraftType.getValue(),
                    parseDouble(fEmptyWeight), parseDouble(fMaxTakeOff),
                    parseDouble(fMaxZeroFuel), parseDouble(fMaxFuel),
                    parseDouble(fServiceCeiling), parseDouble(fCruiseSpeed),
                    parseDouble(fWingArea), parseDouble(fWingSpan),
                    parseDouble(fDragCoef), parseDouble(fLiftCoef),
                    Integer.parseInt(fNumEngines.getText().trim()), engines);
            registerCtrl.registerAircraftModel(dto);
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            clearRegisterForm();
            refresh();
        } catch (final Exception e) {
            registerError.setText(e.getMessage());
        }
    }

    // ── Add Engine ────────────────────────────────────────────────────────────

    @FXML
    private void onAddEngine() {
        hideAllForms();
        addEngineForm.setVisible(true);
        addEngineForm.setManaged(true);
        addEngineError.setText("");
        addAircraftBox.setItems(FXCollections.observableArrayList(
                addCtrl.listAircraftModelsWithRemainingSlots()));
        addEngineBox.setItems(FXCollections.observableArrayList());
    }

    private void refreshAddEngineBox() {
        final AircraftModel selected = addAircraftBox.getValue();
        if (selected == null)
            return;
        addEngineBox.setItems(FXCollections.observableArrayList(
                addCtrl.listAssignableEngineModels(selected)));
    }

    @FXML
    private void onSaveAddEngine() {
        try {
            final AircraftModel aircraft = addAircraftBox.getValue();
            final EngineModel engine = addEngineBox.getValue();
            if (aircraft == null || engine == null) {
                addEngineError.setText("Select both an aircraft model and an engine model.");
                return;
            }
            addCtrl.addEngineModelToAircraft(aircraft.modelName(), aircraft.maker().name(), engine.name());
            addEngineForm.setVisible(false);
            addEngineForm.setManaged(false);
            refresh();
        } catch (final Exception e) {
            addEngineError.setText(e.getMessage());
        }
    }

    @FXML
    private void onCancelAddEngine() {
        addEngineForm.setVisible(false);
        addEngineForm.setManaged(false);
    }

    // ── Remove Engine ─────────────────────────────────────────────────────────

    @FXML
    private void onRemoveEngine() {
        hideAllForms();
        removeEngineForm.setVisible(true);
        removeEngineForm.setManaged(true);
        removeEngineError.setText("");
        // Load all aircraft models that have at least one engine assigned
        final List<AircraftModel> models = new ArrayList<>();
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        PersistenceContext.repositories().aircraftModels().findAll().forEach(m -> {
            if (!m.supportedEngineModels().isEmpty())
                models.add(m);
        });
        removeAircraftBox.setItems(FXCollections.observableArrayList(models));
        removeEngineBox.setItems(FXCollections.observableArrayList());
    }

    private void refreshRemoveEngineBox() {
        final AircraftModel selected = removeAircraftBox.getValue();
        if (selected == null)
            return;
        removeEngineBox.setItems(FXCollections.observableArrayList(
                removeCtrl.listRemovableEngineModels(selected)));
    }

    @FXML
    private void onSaveRemoveEngine() {
        try {
            final AircraftModel aircraft = removeAircraftBox.getValue();
            final EngineModel engine = removeEngineBox.getValue();
            if (aircraft == null || engine == null) {
                removeEngineError.setText("Select both an aircraft model and an engine model.");
                return;
            }
            removeCtrl.removeEngineModel(aircraft, engine.identity());
            removeEngineForm.setVisible(false);
            removeEngineForm.setManaged(false);
            refresh();
        } catch (final Exception e) {
            removeEngineError.setText(e.getMessage());
        }
    }

    @FXML
    private void onCancelRemoveEngine() {
        removeEngineForm.setVisible(false);
        removeEngineForm.setManaged(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void hideAllForms() {
        for (final VBox form : List.of(registerForm, addEngineForm, removeEngineForm)) {
            form.setVisible(false);
            form.setManaged(false);
        }
    }

    private void clearRegisterForm() {
        fModelName.clear();
        fMakerName.clear();
        fMakerCountry.clear();
        fNumEngines.clear();
        fEmptyWeight.clear();
        fMaxTakeOff.clear();
        fMaxZeroFuel.clear();
        fMaxFuel.clear();
        fServiceCeiling.clear();
        fCruiseSpeed.clear();
        fWingArea.clear();
        fWingSpan.clear();
        fDragCoef.clear();
        fLiftCoef.clear();
        fEngineNames.clear();
        fAircraftType.setValue(null);
        registerError.setText("");
    }

    private double parseDouble(final TextField f) {
        try {
            return Double.parseDouble(f.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + f.getPromptText());
        }
    }
}
