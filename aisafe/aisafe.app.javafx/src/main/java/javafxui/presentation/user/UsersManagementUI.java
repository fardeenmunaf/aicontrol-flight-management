package javafxui.presentation.user;

import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colRoles;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colActions;
    @FXML private Label statusLabel;

    @FXML private VBox registerForm;
    @FXML private TextField fName;
    @FXML private TextField fEmail;
    @FXML private TextField fPhone;
    @FXML private PasswordField fPassword;
    @FXML private FlowPane fRolesPane;
    @FXML private Label registerError;

    @FXML private VBox editForm;
    @FXML private Label editEmailLabel;
    @FXML private TextField fEditPhone;
    @FXML private FlowPane fEditRolesPane;
    @FXML private Label editError;

    private String editingEmail;
    private final List<CheckBox> registerRoleBoxes = new ArrayList<>();
    private final List<CheckBox> editRoleBoxes = new ArrayList<>();

    // Controllers are created fresh per operation so each call gets a new EntityManager,
    // preventing stale reads caused by the JPA L1 cache after out-of-band DB changes.
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
            private final Button enableBtn  = new Button("Enable");
            private final Button editBtn    = new Button("Edit");
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

        buildRoleCheckboxes(fRolesPane, registerRoleBoxes);
        buildRoleCheckboxes(fEditRolesPane, editRoleBoxes);

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
        registerRoleBoxes.forEach(cb -> cb.setSelected(false));
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
            registerCtrl().registerUser(
                    fEmail.getText().trim(),
                    fPassword.getText().trim(),
                    fName.getText().trim(),
                    fPhone.getText().trim(),
                    roles);
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
        editRoleBoxes.forEach(cb -> cb.setSelected(user.roles().contains(cb.getUserData())));
        editError.setText("");
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

    // ── Helpers ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Set<Role> selectedRoles(final List<CheckBox> boxes) {
        return boxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Role) cb.getUserData())
                .collect(Collectors.toSet());
    }
}
