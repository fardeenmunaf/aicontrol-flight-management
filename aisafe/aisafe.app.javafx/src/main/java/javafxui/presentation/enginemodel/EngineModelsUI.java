package javafxui.presentation.enginemodel;

import enginemodel.application.DeleteEngineModelController;
import enginemodel.application.ListEngineModelsController;
import enginemodel.application.RegisterEngineModelController;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelType;
import infrastructure.persistence.PersistenceContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafxui.presentation.common.CardListCell;
import javafxui.presentation.common.Cards;

import java.util.ArrayList;
import java.util.List;

public class EngineModelsUI {

    @FXML
    private ListView<EngineModel> table;
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
        table.setCellFactory(lv -> new CardListCell<>(model -> {
            final HBox header = Cards.row(8,
                    Cards.title(model.name()),
                    Cards.grow(),
                    Cards.chip(model.type().name()));

            final String specs = model.maker().name() + " (" + model.maker().country() + ")"
                    + (model.power() != null ? String.format("  ·  %.0f kW", model.power()) : "");

            final Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-danger");
            delete.setOnAction(e -> onDelete(model));

            return Cards.card(header, Cards.meta(specs), Cards.row(8, Cards.grow(), delete));
        }));
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
