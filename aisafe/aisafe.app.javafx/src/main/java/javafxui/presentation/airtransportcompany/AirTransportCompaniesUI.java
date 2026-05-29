package javafxui.presentation.airtransportcompany;

import airtransportcompany.application.ListAirTransportCompaniesController;
import airtransportcompany.application.RegisterAirTransportCompanyController;
import airtransportcompany.domain.AirTransportCompany;
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

public class AirTransportCompaniesUI {

    @FXML
    private TableView<AirTransportCompany> table;
    @FXML
    private TableColumn<AirTransportCompany, String> colName;
    @FXML
    private TableColumn<AirTransportCompany, String> colIata;
    @FXML
    private TableColumn<AirTransportCompany, String> colIcao;
    @FXML
    private TableColumn<AirTransportCompany, String> colAction;
    @FXML
    private Label statusLabel;

    @FXML
    private VBox registerForm;
    @FXML
    private TextField fName, fIata, fIcao;
    @FXML
    private Label formError;

    private final ListAirTransportCompaniesController listCtrl = new ListAirTransportCompaniesController(
            PersistenceContext.repositories().airTransportCompanies());
    private final RegisterAirTransportCompanyController registerCtrl = new RegisterAirTransportCompanyController(
            PersistenceContext.repositories().airTransportCompanies());
    private final ObservableList<AirTransportCompany> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colIata.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().iataCode()));
        colIcao.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().icaoCode()));
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
        final List<AirTransportCompany> list = listCtrl.allCompanies();
        data.setAll(list);
        statusLabel.setText(list.size() + " companies");
    }

    private void onDelete(final AirTransportCompany company) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete company \"" + company.name() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
                PersistenceContext.repositories().airTransportCompanies().delete(company);
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
        fName.clear();
        fIata.clear();
        fIcao.clear();
        formError.setText("");
    }

    @FXML
    private void onSaveRegister() {
        try {
            registerCtrl.registerCompany(
                    fName.getText().trim(),
                    fIata.getText().trim().toUpperCase(),
                    fIcao.getText().trim().toUpperCase());
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            fName.clear();
            fIata.clear();
            fIcao.clear();
            refresh();
        } catch (final Exception e) {
            formError.setText(e.getMessage());
        }
    }
}
