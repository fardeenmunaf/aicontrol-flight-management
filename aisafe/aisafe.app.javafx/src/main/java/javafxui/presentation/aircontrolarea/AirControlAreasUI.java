package javafxui.presentation.aircontrolarea;

import aircontrolarea.application.ListAirControlAreasController;
import aircontrolarea.application.RegisterAirControlAreaController;
import aircontrolarea.domain.AirControlArea;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafxui.presentation.common.CardListCell;
import javafxui.presentation.common.Cards;

import java.util.List;

public class AirControlAreasUI {

    @FXML
    private ListView<AirControlArea> table;
    @FXML
    private Label statusLabel;

    @FXML
    private VBox registerForm;
    @FXML
    private TextField fCode, fMinFuel, fBoundaries;
    @FXML
    private Label formError;

    private final ListAirControlAreasController listCtrl = new ListAirControlAreasController(
            PersistenceContext.repositories().airControlAreas());
    private final RegisterAirControlAreaController registerCtrl = new RegisterAirControlAreaController(
            PersistenceContext.repositories().airControlAreas());
    private final ObservableList<AirControlArea> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        table.setCellFactory(lv -> new CardListCell<>(area -> {
            final HBox header = Cards.row(8,
                    Cards.title(area.areaCode()),
                    Cards.grow(),
                    Cards.chip(String.format("%.0f kg min fuel", area.minimumRequiredArrivalFuel())));

            final Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-danger");
            delete.setOnAction(e -> onDelete(area));

            return Cards.card(header, Cards.meta(area.geographicBoundaries()),
                    Cards.row(8, Cards.grow(), delete));
        }));
        table.setItems(data);
        refresh();
    }

    private void refresh() {
        final List<AirControlArea> list = listCtrl.allAirControlAreas();
        data.setAll(list);
        statusLabel.setText(list.size() + " air control areas");
    }

    private void onDelete(final AirControlArea area) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete air control area \"" + area.areaCode() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
                PersistenceContext.repositories().airControlAreas().delete(area);
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
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
        fCode.clear();
        fMinFuel.clear();
        fBoundaries.clear();
        formError.setText("");
    }

    @FXML
    private void onSaveRegister() {
        try {
            final double minFuel = Double.parseDouble(fMinFuel.getText().trim());
            registerCtrl.registerAirControlArea(
                    fCode.getText().trim(), minFuel, fBoundaries.getText().trim());
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            fCode.clear();
            fMinFuel.clear();
            fBoundaries.clear();
            refresh();
        } catch (final NumberFormatException e) {
            formError.setText("Minimum fuel must be a valid number.");
        } catch (final Exception e) {
            formError.setText(e.getMessage());
        }
    }
}
