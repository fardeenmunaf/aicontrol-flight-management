package javafxui.presentation.airtransportcompany;

import airtransportcompany.application.ListAirTransportCompaniesController;
import airtransportcompany.application.RegisterAirTransportCompanyController;
import airtransportcompany.domain.AirTransportCompany;
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

public class AirTransportCompaniesUI {

    @FXML
    private ListView<AirTransportCompany> table;
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
        table.setCellFactory(lv -> new CardListCell<>(company -> {
            final HBox header = Cards.row(8,
                    Cards.title(company.name()),
                    Cards.grow(),
                    Cards.chip(company.iataCode()),
                    Cards.chip(company.icaoCode(), "chip-neutral"));

            final Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-danger");
            delete.setOnAction(e -> onDelete(company));

            return Cards.card(header, Cards.row(8, Cards.grow(), delete));
        }));
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
