package javafxui.presentation.customercollaborator;

import aircontrolarea.domain.AirControlArea;
import airtransportcompany.domain.AirTransportCompany;
import auth.domain.AISafeRoles;
import customercollaborator.application.DisableCustomerCollaboratorController;
import customercollaborator.application.RegisterCustomerCollaboratorController;
import customercollaborator.application.UpdateCustomerCollaboratorController;
import customercollaborator.domain.CustomerCollaborator;
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
import java.util.List;

public class CustomerCollaboratorsUI {

    @FXML
    private TableView<CustomerCollaborator> table;
    @FXML
    private TableColumn<CustomerCollaborator, String> colUser;
    @FXML
    private TableColumn<CustomerCollaborator, String> colType;
    @FXML
    private TableColumn<CustomerCollaborator, String> colPosition;
    @FXML
    private TableColumn<CustomerCollaborator, String> colCompany;
    @FXML
    private TableColumn<CustomerCollaborator, String> colArea;
    @FXML
    private TableColumn<CustomerCollaborator, String> colActive;
    @FXML
    private TableColumn<CustomerCollaborator, String> colActions;
    @FXML
    private Label statusLabel;

    // Register form
    @FXML
    private VBox registerForm;
    @FXML
    private ComboBox<String> fCollabType;
    @FXML
    private ComboBox<AirTransportCompany> fCompany;
    @FXML
    private ComboBox<AirControlArea> fArea;
    @FXML
    private Label companyLabel, areaLabel;
    @FXML
    private TextField fEmail, fPassword, fName, fPhone, fPosition;
    @FXML
    private Label registerError;

    // Update form
    @FXML
    private VBox updateForm;
    @FXML
    private Label updateUserIdLabel;
    @FXML
    private TextField fNewEmail, fNewPhone;
    @FXML
    private Label updateError;

    private String updateUserId;

    private final RegisterCustomerCollaboratorController registerCtrl = new RegisterCustomerCollaboratorController(
            PersistenceContext.repositories().users(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().airControlAreas());
    private final DisableCustomerCollaboratorController disableCtrl = new DisableCustomerCollaboratorController(
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().users());
    private final UpdateCustomerCollaboratorController updateCtrl = new UpdateCustomerCollaboratorController(
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().users());

    private final ObservableList<CustomerCollaborator> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().userId().toString()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().customerType() != null ? c.getValue().customerType().name() : "—"));
        colPosition.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().position()));
        colCompany.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().airTransportCompanyId() != null
                        ? c.getValue().airTransportCompanyId().toString()
                        : "—"));
        colArea.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().airControlAreaId() != null
                        ? c.getValue().airControlAreaId().toString()
                        : "—"));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isActive() ? "Active" : "Disabled"));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button disableBtn = new Button("Disable");
            private final Button updateBtn = new Button("Update");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, disableBtn, updateBtn);
            {
                disableBtn.getStyleClass().add("btn-danger");
                disableBtn.setOnAction(e -> onDisable(getTableView().getItems().get(getIndex())));
                updateBtn.getStyleClass().add("btn-primary");
                updateBtn.setOnAction(e -> onOpenUpdate(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(data);

        fCollabType.setItems(FXCollections.observableArrayList("Air Transport Company", "Air Control Area"));
        fCollabType.setOnAction(e -> onTypeChanged());

        fCompany.setConverter(new StringConverter<>() {
            @Override
            public String toString(final AirTransportCompany c) {
                return c == null ? "" : c.name() + " (" + c.iataCode() + ")";
            }

            @Override
            public AirTransportCompany fromString(final String s) {
                return null;
            }
        });
        fArea.setConverter(new StringConverter<>() {
            @Override
            public String toString(final AirControlArea a) {
                return a == null ? "" : a.areaCode();
            }

            @Override
            public AirControlArea fromString(final String s) {
                return null;
            }
        });

        refresh();
    }

    private void refresh() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        final List<CustomerCollaborator> list = new ArrayList<>();
        PersistenceContext.repositories().customerCollaborators().findAll().forEach(list::add);
        data.setAll(list);
        statusLabel.setText(list.size() + " collaborators");
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @FXML
    private void onRegister() {
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        updateForm.setVisible(false);
        updateForm.setManaged(false);
        registerError.setText("");
        fCollabType.setValue(null);
        setCompanyAreaVisibility(false, false);
        loadCompaniesAndAreas();
    }

    private void loadCompaniesAndAreas() {
        final List<AirTransportCompany> companies = new ArrayList<>();
        PersistenceContext.repositories().airTransportCompanies().findAll().forEach(companies::add);
        fCompany.setItems(FXCollections.observableArrayList(companies));

        final List<AirControlArea> areas = new ArrayList<>();
        PersistenceContext.repositories().airControlAreas().findAll().forEach(areas::add);
        fArea.setItems(FXCollections.observableArrayList(areas));
    }

    private void onTypeChanged() {
        final String type = fCollabType.getValue();
        if ("Air Transport Company".equals(type)) {
            setCompanyAreaVisibility(true, false);
        } else if ("Air Control Area".equals(type)) {
            setCompanyAreaVisibility(false, true);
        }
    }

    private void setCompanyAreaVisibility(final boolean showCompany, final boolean showArea) {
        companyLabel.setVisible(showCompany);
        companyLabel.setManaged(showCompany);
        fCompany.setVisible(showCompany);
        fCompany.setManaged(showCompany);
        areaLabel.setVisible(showArea);
        areaLabel.setManaged(showArea);
        fArea.setVisible(showArea);
        fArea.setManaged(showArea);
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
            final String type = fCollabType.getValue();
            if ("Air Transport Company".equals(type)) {
                final AirTransportCompany company = fCompany.getValue();
                if (company == null) {
                    registerError.setText("Select a company.");
                    return;
                }
                registerCtrl.registerForAirTransportCompany(
                        company.identity().toString(),
                        fEmail.getText().trim(), fPassword.getText().trim(),
                        fName.getText().trim(), fPhone.getText().trim(), fPosition.getText().trim());
            } else if ("Air Control Area".equals(type)) {
                final AirControlArea area = fArea.getValue();
                if (area == null) {
                    registerError.setText("Select an air control area.");
                    return;
                }
                registerCtrl.registerForAirControlArea(
                        area.identity().toString(),
                        fEmail.getText().trim(), fPassword.getText().trim(),
                        fName.getText().trim(), fPhone.getText().trim(), fPosition.getText().trim());
            } else {
                registerError.setText("Select a collaborator type.");
                return;
            }
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            clearRegisterForm();
            refresh();
        } catch (final Exception e) {
            registerError.setText(e.getMessage());
        }
    }

    // ── Disable ───────────────────────────────────────────────────────────────

    private void onDisable(final CustomerCollaborator c) {
        if (!c.isActive()) {
            statusLabel.setText("Collaborator is already disabled.");
            return;
        }
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Disable collaborator \"" + c.userId() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                disableCtrl.disable(c.userId().toString());
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    // ── Update ────────────────────────────────────────────────────────────────

    private void onOpenUpdate(final CustomerCollaborator c) {
        updateUserId = c.userId().toString();
        updateUserIdLabel.setText("Updating: " + updateUserId);
        fNewEmail.clear();
        fNewPhone.clear();
        updateError.setText("");
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        updateForm.setVisible(true);
        updateForm.setManaged(true);
    }

    @FXML
    private void onSaveUpdate() {
        try {
            updateCtrl.updateCollaboratorContactInfo(
                    updateUserId,
                    fNewEmail.getText().trim().isEmpty() ? null : fNewEmail.getText().trim(),
                    fNewPhone.getText().trim().isEmpty() ? null : fNewPhone.getText().trim());
            updateForm.setVisible(false);
            updateForm.setManaged(false);
            refresh();
        } catch (final Exception e) {
            updateError.setText(e.getMessage());
        }
    }

    @FXML
    private void onCancelUpdate() {
        updateForm.setVisible(false);
        updateForm.setManaged(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearRegisterForm() {
        fCollabType.setValue(null);
        fCompany.setValue(null);
        fArea.setValue(null);
        fEmail.clear();
        fPassword.clear();
        fName.clear();
        fPhone.clear();
        fPosition.clear();
        registerError.setText("");
        setCompanyAreaVisibility(false, false);
    }
}
