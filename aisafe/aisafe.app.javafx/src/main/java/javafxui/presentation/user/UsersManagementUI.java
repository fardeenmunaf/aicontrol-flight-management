package javafxui.presentation.user;

import aircraftmodel.domain.AircraftModel;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import infrastructure.persistence.PersistenceContext;
import javafx.util.StringConverter;
import shared.domain.AirTransportCompanyId;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import shared.domain.UserId;
import user.application.DisableEnableUserController;
import user.application.ListUsersController;
import user.application.RegisterUserController;
import user.domain.Role;
import user.domain.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UsersManagementUI {

    @FXML
    private TableView<User> table;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colPhone;
    @FXML
    private TableColumn<User, String> colRoles;
    @FXML
    private TableColumn<User, String> colStatus;
    @FXML
    private TableColumn<User, String> colActions;
    @FXML
    private Label statusLabel;

    @FXML
    private VBox registerForm;
    @FXML
    private TextField fName;
    @FXML
    private TextField fEmail;
    @FXML
    private TextField fPhone;
    @FXML
    private PasswordField fPassword;
    @FXML
    private FlowPane fRolesPane;
    @FXML
    private VBox pilotRegisterRow;
    @FXML
    private FlowPane fPilotModelsPane;
    @FXML
    private HBox atccRegisterRow;
    @FXML
    private ComboBox<AirTransportCompany> fAtccCompany;
    @FXML
    private TextField fAtccPosition;
    @FXML
    private Label registerError;

    @FXML
    private VBox editForm;
    @FXML
    private Label editEmailLabel;
    @FXML
    private TextField fEditPhone;
    @FXML
    private FlowPane fEditRolesPane;
    @FXML
    private VBox pilotEditRow;
    @FXML
    private FlowPane fEditModelsPane;
    @FXML
    private HBox atccEditRow;
    @FXML
    private ComboBox<AirTransportCompany> fEditAtccCompany;
    @FXML
    private TextField fEditAtccPosition;
    @FXML
    private Label editAtccInfo;
    @FXML
    private Label editError;

    private String editingEmail;
    private final List<CheckBox> registerRoleBoxes = new ArrayList<>();
    private final List<CheckBox> editRoleBoxes = new ArrayList<>();
    private final List<CheckBox> registerModelBoxes = new ArrayList<>();
    private final List<CheckBox> editModelBoxes = new ArrayList<>();

    private RegisterUserController registerCtrl() {
        return new RegisterUserController(PersistenceContext.repositories().users());
    }

    private ListUsersController listCtrl() {
        return new ListUsersController(PersistenceContext.repositories().users());
    }

    private DisableEnableUserController disableCtrl() {
        return new DisableEnableUserController(PersistenceContext.repositories().users());
    }

    private final ObservableList<User> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().phoneNumber()));
        colRoles.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().roles().stream()
                        .map(Role::designation)
                        .sorted()
                        .collect(Collectors.joining(", "))));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button disableBtn = new Button("Disable");
            private final Button enableBtn = new Button("Enable");
            private final Button editBtn = new Button("Edit");
            private final HBox box = new HBox(6, disableBtn, enableBtn, editBtn);
            {
                disableBtn.getStyleClass().add("btn-danger");
                enableBtn.getStyleClass().add("btn-success");
                editBtn.getStyleClass().add("btn-primary");
                disableBtn.setOnAction(e -> onDisable(getTableView().getItems().get(getIndex())));
                enableBtn.setOnAction(e -> onEnable(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> onOpenEdit(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                final User user = getTableView().getItems().get(getIndex());
                disableBtn.setVisible(user.isActive());
                disableBtn.setManaged(user.isActive());
                enableBtn.setVisible(!user.isActive());
                enableBtn.setManaged(!user.isActive());
                setGraphic(box);
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(data);

        final StringConverter<AirTransportCompany> companyConverter = new StringConverter<>() {
            @Override
            public String toString(final AirTransportCompany c) {
                return c == null ? "" : c.name() + " (" + c.iataCode() + ")";
            }

            @Override
            public AirTransportCompany fromString(final String s) {
                return null;
            }
        };
        fAtccCompany.setConverter(companyConverter);
        fEditAtccCompany.setConverter(companyConverter);

        buildRoleCheckboxes(fRolesPane, registerRoleBoxes);
        buildRoleCheckboxes(fEditRolesPane, editRoleBoxes);

        setupPilotListener(registerRoleBoxes, pilotRegisterRow, fPilotModelsPane, registerModelBoxes);
        setupPilotListener(editRoleBoxes, pilotEditRow, fEditModelsPane, editModelBoxes);
        setupAtccListener(registerRoleBoxes, atccRegisterRow, fAtccCompany, fAtccPosition);
        setupAtccListener(editRoleBoxes, atccEditRow, fEditAtccCompany, fEditAtccPosition);

        refresh();
    }

    private void buildRoleCheckboxes(final FlowPane pane, final List<CheckBox> boxes) {
        pane.getChildren().clear();
        boxes.clear();
        for (final Role role : Role.values()) {
            final CheckBox cb = new CheckBox(role.designation());
            cb.setUserData(role);
            cb.setStyle("-fx-padding: 2 12 2 0;");
            boxes.add(cb);
            pane.getChildren().add(cb);
        }
    }

    private void setupPilotListener(final List<CheckBox> roleBoxes, final VBox pilotRow,
            final FlowPane modelsPane, final List<CheckBox> modelBoxes) {
        roleBoxes.stream()
                .filter(cb -> cb.getUserData() == Role.PILOT)
                .findFirst()
                .ifPresent(pilotBox -> pilotBox.selectedProperty().addListener((obs, was, isNow) -> {
                    if (isNow) {
                        loadAircraftModelCheckboxes(modelsPane, modelBoxes);
                        pilotRow.setVisible(true);
                        pilotRow.setManaged(true);
                    } else {
                        pilotRow.setVisible(false);
                        pilotRow.setManaged(false);
                        modelBoxes.forEach(cb -> cb.setSelected(false));
                    }
                }));
    }

    private void setupAtccListener(final List<CheckBox> roleBoxes, final HBox atccRow,
            final ComboBox<AirTransportCompany> companyCb,
            final TextField positionField) {
        roleBoxes.stream()
                .filter(cb -> cb.getUserData() == Role.AIR_TRANSPORT_COMPANY_COLLABORATOR)
                .findFirst()
                .ifPresent(atccBox -> atccBox.selectedProperty().addListener((obs, was, isNow) -> {
                    if (isNow) {
                        loadCompanies(companyCb);
                        atccRow.setVisible(true);
                        atccRow.setManaged(true);
                    } else {
                        atccRow.setVisible(false);
                        atccRow.setManaged(false);
                        companyCb.setValue(null);
                        positionField.clear();
                    }
                }));
    }

    private void loadCompanies(final ComboBox<AirTransportCompany> target) {
        final List<AirTransportCompany> companies = new ArrayList<>();
        PersistenceContext.repositories().airTransportCompanies().findAll().forEach(companies::add);
        target.setItems(FXCollections.observableArrayList(companies));
    }

    private void loadAircraftModelCheckboxes(final FlowPane pane, final List<CheckBox> boxes) {
        pane.getChildren().clear();
        boxes.clear();
        PersistenceContext.repositories().aircraftModels().findAll().forEach(model -> {
            final CheckBox cb = new CheckBox(model.modelName());
            cb.setUserData(model);
            cb.setStyle("-fx-padding: 2 12 2 0;");
            boxes.add(cb);
            pane.getChildren().add(cb);
        });
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        final List<User> list = listCtrl().allUsers();
        data.setAll(list);
        statusLabel.setText(list.size() + " users");
    }

    // ── Register ──────────────────────────────────────────────────────────

    @FXML
    private void onRegister() {
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        editForm.setVisible(false);
        editForm.setManaged(false);
        registerError.setText("");
        fName.clear();
        fEmail.clear();
        fPhone.clear();
        fPassword.clear();
        pilotRegisterRow.setVisible(false);
        pilotRegisterRow.setManaged(false);
        atccRegisterRow.setVisible(false);
        atccRegisterRow.setManaged(false);
        fAtccCompany.setValue(null);
        fAtccPosition.clear();
        registerRoleBoxes.forEach(cb -> cb.setSelected(false));
        registerModelBoxes.forEach(cb -> cb.setSelected(false));
    }

    @FXML
    private void onCancelRegister() {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
    }

    @FXML
    private void onSaveRegister() {
        try {
            final Set<Role> roles = selectedRoles(registerRoleBoxes);
            if (roles.isEmpty()) {
                registerError.setText("Select at least one role.");
                return;
            }
            final String email = fEmail.getText().trim();
            registerCtrl().registerUser(email, fPassword.getText().trim(),
                    fName.getText().trim(), fPhone.getText().trim(), roles);

            if (roles.contains(Role.PILOT)) {
                savePilotRecord(email, selectedModels(registerModelBoxes));
            }

            if (roles.contains(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
                final AirTransportCompany company = fAtccCompany.getValue();
                if (company == null) {
                    registerError.setText("Select an Air Transport Company for the ATCC role.");
                    return;
                }
                final String position = fAtccPosition.getText().trim();
                if (position.isEmpty()) {
                    registerError.setText("Position is required for the ATCC role.");
                    return;
                }
                PersistenceContext.repositories().customerCollaborators().save(
                        new CustomerCollaborator(UserId.valueOf(email),
                                CustomerType.AIR_TRANSPORT_COMPANY,
                                AirTransportCompanyId.valueOf(company.identity().toString()),
                                null, position));
            }

            registerForm.setVisible(false);
            registerForm.setManaged(false);
            refresh();
        } catch (final Exception e) {
            registerError.setText(e.getMessage());
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────

    private void onOpenEdit(final User user) {
        editingEmail = user.email();
        editEmailLabel.setText("Editing: " + editingEmail);
        fEditPhone.setText(user.phoneNumber());
        editError.setText("");
        pilotEditRow.setVisible(false);
        pilotEditRow.setManaged(false);
        atccEditRow.setVisible(false);
        atccEditRow.setManaged(false);
        editAtccInfo.setVisible(false);
        editAtccInfo.setManaged(false);
        fEditAtccCompany.setValue(null);
        fEditAtccPosition.clear();

        editRoleBoxes.forEach(cb -> cb.setSelected(user.roles().contains(cb.getUserData())));

        if (user.roles().contains(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            loadCompanies(fEditAtccCompany);
            PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(UserId.valueOf(editingEmail))
                    .ifPresent(collab -> {
                        if (collab.airTransportCompanyId() != null) {
                            final String cid = collab.airTransportCompanyId().toString();
                            fEditAtccCompany.getItems().stream()
                                    .filter(c -> c.identity().toString().equals(cid))
                                    .findFirst()
                                    .ifPresent(fEditAtccCompany::setValue);
                            fEditAtccPosition.setText(collab.position());
                            editAtccInfo.setText("Already linked — saving will not change the existing association.");
                            editAtccInfo.setVisible(true);
                            editAtccInfo.setManaged(true);
                        }
                    });
            atccEditRow.setVisible(true);
            atccEditRow.setManaged(true);
        }

        if (user.roles().contains(Role.PILOT)) {
            loadAircraftModelCheckboxes(fEditModelsPane, editModelBoxes);
            preselectPilotModels(editingEmail, editModelBoxes);
            pilotEditRow.setVisible(true);
            pilotEditRow.setManaged(true);
        }

        registerForm.setVisible(false);
        registerForm.setManaged(false);
        editForm.setVisible(true);
        editForm.setManaged(true);
    }

    @FXML
    private void onSaveEdit() {
        try {
            final Set<Role> roles = selectedRoles(editRoleBoxes);
            if (roles.isEmpty()) {
                editError.setText("Select at least one role.");
                return;
            }
            registerCtrl().editUser(editingEmail, fEditPhone.getText().trim(), roles);

            if (roles.contains(Role.PILOT)) {
                savePilotRecord(editingEmail, selectedModels(editModelBoxes));
            }

            if (roles.contains(Role.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
                final boolean alreadyLinked = PersistenceContext.repositories().customerCollaborators()
                        .findByUserId(UserId.valueOf(editingEmail)).isPresent();
                if (!alreadyLinked) {
                    final AirTransportCompany company = fEditAtccCompany.getValue();
                    if (company == null) {
                        editError.setText("Select an Air Transport Company for the ATCC role.");
                        return;
                    }
                    final String position = fEditAtccPosition.getText().trim();
                    if (position.isEmpty()) {
                        editError.setText("Position is required for the ATCC role.");
                        return;
                    }
                    PersistenceContext.repositories().customerCollaborators().save(
                            new CustomerCollaborator(UserId.valueOf(editingEmail),
                                    CustomerType.AIR_TRANSPORT_COMPANY,
                                    AirTransportCompanyId.valueOf(company.identity().toString()),
                                    null, position));
                }
            }

            editForm.setVisible(false);
            editForm.setManaged(false);
            refresh();
        } catch (final Exception e) {
            editError.setText(e.getMessage());
        }
    }

    @FXML
    private void onCancelEdit() {
        editForm.setVisible(false);
        editForm.setManaged(false);
    }

    // ── Disable / Enable ──────────────────────────────────────────────────

    private void onDisable(final User user) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Disable user \"" + user.email() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                disableCtrl().disableUser(user.email());
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void onEnable(final User user) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Enable user \"" + user.email() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                disableCtrl().enableUser(user.email());
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    // ── Pilot helpers ─────────────────────────────────────────────────────

    private void savePilotRecord(final String email, final Set<AircraftModel> models) {
        final var repo = PersistenceContext.repositories().pilots();
        final List<Pilot> all = new ArrayList<>();
        repo.findAll().forEach(all::add);
        // Replace existing record so certified models stay current
        all.stream()
                .filter(p -> p.userId().toString().equals(email))
                .forEach(repo::delete);
        repo.save(new Pilot(UserId.valueOf(email), PilotStatus.ACTIVE, models));
    }

    private void preselectPilotModels(final String email, final List<CheckBox> boxes) {
        final List<Pilot> all = new ArrayList<>();
        PersistenceContext.repositories().pilots().findAll().forEach(all::add);
        final Set<String> certifiedNames = all.stream()
                .filter(p -> p.userId().toString().equals(email))
                .flatMap(p -> p.certifiedAircraftModels().stream())
                .map(AircraftModel::modelName)
                .collect(Collectors.toSet());
        boxes.forEach(cb -> cb.setSelected(certifiedNames.contains(cb.getText())));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Set<Role> selectedRoles(final List<CheckBox> boxes) {
        return boxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Role) cb.getUserData())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private Set<AircraftModel> selectedModels(final List<CheckBox> boxes) {
        return boxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (AircraftModel) cb.getUserData())
                .collect(Collectors.toSet());
    }
}
