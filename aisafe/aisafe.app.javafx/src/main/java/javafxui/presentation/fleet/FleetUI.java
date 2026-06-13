package javafxui.presentation.fleet;

import aircraft.application.DecommissionAircraftController;
import aircraft.application.ListAircraftController;
import aircraft.application.RegisterAircraftController;
import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
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

public class FleetUI {

    @FXML
    private ListView<Aircraft> fleetTable;
    @FXML
    private Label messageLabel;
    @FXML
    private VBox registerForm;
    @FXML
    private TextField fReg, fCountry, fCrew, fEcon, fBiz, fFirst, fModel, fEngine;
    @FXML
    private Label formError;

    private final ListAircraftController listCtrl = new ListAircraftController(
            PersistenceContext.repositories().aircraft());
    private final DecommissionAircraftController decommCtrl = new DecommissionAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().flights());
    private final RegisterAircraftController registerCtrl = new RegisterAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().engineModels());

    private final ObservableList<Aircraft> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        fleetTable.setCellFactory(lv -> new CardListCell<>(aircraft -> {
            final AircraftOperationStatus status = aircraft.operationStatus();
            final String statusChipClass = status == AircraftOperationStatus.IN_SERVICE
                    ? "chip-success"
                    : status == AircraftOperationStatus.MAINTENANCE ? "chip-warning" : "chip-danger";
            final HBox header = Cards.row(8,
                    Cards.title(aircraft.registrationNumber()),
                    Cards.grow(),
                    Cards.chip(status.name(), statusChipClass));

            final String model = aircraft.aircraftModel() != null
                    ? aircraft.aircraftModel().modelName() : "—";
            final var cab = aircraft.cabinConfiguration();
            final String cabin = cab != null
                    ? cab.economyClassSeats() + "E / " + cab.businessClassSeats() + "B / " + cab.firstClassSeats() + "F"
                    : "—";
            final String details = model + "  ·  " + cabin + "  ·  " + aircraft.registrationCountry();

            final boolean decomm = status == AircraftOperationStatus.DECOMISSIONED;
            final Button statusBtn = new Button(decomm ? "Reactivate" : "Decommission");
            statusBtn.getStyleClass().add(decomm ? "btn-success" : "btn-danger");
            statusBtn.setOnAction(e -> {
                if (decomm) {
                    onReactivate(aircraft);
                } else {
                    onDecommission(aircraft);
                }
            });
            final Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("btn-danger");
            deleteBtn.setOnAction(e -> onDelete(aircraft));

            return Cards.card(header, Cards.meta(details),
                    Cards.row(8, Cards.grow(), statusBtn, deleteBtn));
        }));
        fleetTable.setItems(data);
        refreshTable();
    }

    private void refreshTable() {
        final List<Aircraft> list = new ArrayList<>();
        listCtrl.listAll().forEach(list::add);
        data.setAll(list);
        fleetTable.refresh();
        messageLabel.setText(list.size() + " aircraft");
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
            registerCtrl.registerAircraft(
                    fReg.getText().trim(),
                    fCountry.getText().trim(),
                    AircraftOperationStatus.IN_SERVICE,
                    Integer.parseInt(fCrew.getText().trim()),
                    fModel.getText().trim(),
                    fEngine.getText().trim(),
                    Integer.parseInt(fEcon.getText().trim()),
                    Integer.parseInt(fBiz.getText().trim()),
                    Integer.parseInt(fFirst.getText().trim()));
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            clearForm();
            refreshTable();
        } catch (final Exception e) {
            formError.setText(e.getMessage());
        }
    }

    private void onDecommission(final Aircraft aircraft) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Decommission aircraft \"" + aircraft.registrationNumber() + "\"?\nThis will retire it from service.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Decommission");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                decommCtrl.decommissionAircraft(aircraft.identity().toString());
                final var repo = PersistenceContext.repositories().aircraft();
                final Aircraft managed = repo.ofIdentity(aircraft.identity())
                        .orElseThrow(() -> new IllegalStateException("Aircraft not found"));
                final int idx = data.indexOf(aircraft);
                if (idx >= 0)
                    data.set(idx, managed);
                else
                    refreshTable();
                messageLabel.setText("Aircraft decommissioned.");
            } catch (final Exception e) {
                messageLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void onReactivate(final Aircraft aircraft) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reactivate aircraft \"" + aircraft.registrationNumber() + "\"?\nThis will return it to service.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Reactivation");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                final var repo = PersistenceContext.repositories().aircraft();
                final Aircraft managed = repo.ofIdentity(aircraft.identity())
                        .orElseThrow(() -> new IllegalStateException("Aircraft not found"));
                managed.reactivate();
                repo.save(managed);
                final int idx = data.indexOf(aircraft);
                if (idx >= 0)
                    data.set(idx, managed);
                else
                    refreshTable();
                messageLabel.setText("Aircraft reactivated.");
            } catch (final Exception e) {
                messageLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void onDelete(final Aircraft aircraft) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete aircraft \"" + aircraft.registrationNumber() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                PersistenceContext.repositories().aircraft().delete(aircraft);
                data.remove(aircraft);
                messageLabel.setText(data.size() + " aircraft");
            } catch (final Exception e) {
                messageLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void clearForm() {
        fReg.clear();
        fCountry.clear();
        fCrew.clear();
        fEcon.clear();
        fBiz.clear();
        fFirst.clear();
        fModel.clear();
        fEngine.clear();
        formError.setText("");
    }
}
