package javafxui.presentation.aircontrolarea;

import aircontrolarea.application.ListAirControlAreasController;
import aircontrolarea.application.RegisterAirControlAreaController;
import aircontrolarea.domain.AirControlArea;
import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class AirControlAreasUI {

    @FXML
    private TableView<AirControlArea> table;
    @FXML
    private TableColumn<AirControlArea, String> colCode;
    @FXML
    private TableColumn<AirControlArea, String> colMinFuel;
    @FXML
    private TableColumn<AirControlArea, String> colBoundaries;
    @FXML
    private TableColumn<AirControlArea, String> colAction;
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
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().areaCode()));
        colMinFuel.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.0f kg", c.getValue().minimumRequiredArrivalFuel())));
        colBoundaries.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().geographicBoundaries()));
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
