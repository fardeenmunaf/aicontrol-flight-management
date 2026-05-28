package javafxui.presentation.enginemodel;

import enginemodel.application.DeleteEngineModelController;
import enginemodel.application.ListEngineModelsController;
import enginemodel.application.RegisterEngineModelController;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelType;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class EngineModelsUI {

    @FXML
    private TableView<EngineModel> table;
    @FXML
    private TableColumn<EngineModel, String> colName;
    @FXML
    private TableColumn<EngineModel, String> colType;
    @FXML
    private TableColumn<EngineModel, String> colMaker;
    @FXML
    private TableColumn<EngineModel, String> colPower;
    @FXML
    private TableColumn<EngineModel, String> colAction;
    @FXML
    private Label statusLabel;

    @FXML
    private VBox registerForm;
    @FXML
    private TextField fName, fPower, fEfficiency, fThrustSLS, fThrustSLC, fTSFC;
    @FXML
    private TextField fMakerName, fMakerCountry, fFuelDensity, fFuelSpecificEnergy;
    @FXML
    private ComboBox<EngineType> fType;
    @FXML
    private ComboBox<FuelType> fFuelType;
    @FXML
    private Label formError;

    private final ListEngineModelsController listCtrl = new ListEngineModelsController(
            PersistenceContext.repositories().engineModels());
    private final RegisterEngineModelController registerCtrl = new RegisterEngineModelController(
            PersistenceContext.repositories().engineModels());
    private final DeleteEngineModelController deleteCtrl = new DeleteEngineModelController(
            PersistenceContext.repositories().engineModels());
    private final ObservableList<EngineModel> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type().name()));
        colMaker.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().maker().name() + " (" + c.getValue().maker().country() + ")"));
        colPower.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().power() != null ? String.format("%.0f kW", c.getValue().power()) : "—"));
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
        fType.setItems(FXCollections.observableArrayList(EngineType.values()));
        fFuelType.setItems(FXCollections.observableArrayList(FuelType.values()));
        refresh();
    }

    private void refresh() {
        final List<EngineModel> list = new ArrayList<>();
        listCtrl.listEngineModels().forEach(list::add);
        data.setAll(list);
        statusLabel.setText(list.size() + " engine models");
    }

    @FXML
    private void onRegister() {
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        formError.setText("");
    }

    @FXML
    private void onCancelRegister() {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        clearForm();
    }

    @FXML
    private void onSaveRegister() {
        try {
            registerCtrl.registerEngineModel(
                    fName.getText().trim(), fType.getValue(),
                    parseDouble(fPower), parseDouble(fEfficiency),
                    parseDouble(fThrustSLS), parseDouble(fThrustSLC), parseDouble(fTSFC),
                    fMakerName.getText().trim(), fMakerCountry.getText().trim(),
                    fFuelType.getValue(), parseDouble(fFuelDensity), parseDouble(fFuelSpecificEnergy));
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            clearForm();
            refresh();
        } catch (final Exception e) {
            formError.setText(e.getMessage());
        }
    }

    private void onDelete(final EngineModel model) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete engine model \"" + model.name() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                deleteCtrl.deleteEngineModel(model);
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void clearForm() {
        fName.clear();
        fPower.clear();
        fEfficiency.clear();
        fThrustSLS.clear();
        fThrustSLC.clear();
        fTSFC.clear();
        fMakerName.clear();
        fMakerCountry.clear();
        fFuelDensity.clear();
        fFuelSpecificEnergy.clear();
        fType.setValue(null);
        fFuelType.setValue(null);
        formError.setText("");
    }

    private double parseDouble(final TextField f) {
        try {
            return Double.parseDouble(f.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in field: " + f.getPromptText());
        }
    }
}
