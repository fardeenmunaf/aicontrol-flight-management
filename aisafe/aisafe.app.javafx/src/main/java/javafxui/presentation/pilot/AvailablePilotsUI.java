package javafxui.presentation.pilot;

import customercollaborator.domain.CustomerCollaborator;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import jpa.JpaPilotCompanyAssignmentRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotCompanyAssignment;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AvailablePilotsUI {

    @FXML private Label pageTitle;
    @FXML private Label statusLabel;
    @FXML private TableView<Pilot> table;
    @FXML private TableColumn<Pilot, String> colEmail;
    @FXML private TableColumn<Pilot, String> colStatus;
    @FXML private TableColumn<Pilot, String> colModels;
    @FXML private TableColumn<Pilot, String> colAction;

    private AirTransportCompanyId myCompanyId;
    private final ObservableList<Pilot> data = FXCollections.observableArrayList();

    private JpaPilotCompanyAssignmentRepository assignments() {
        return new JpaPilotCompanyAssignmentRepository("aisafe-pu");
    }

    @FXML
    public void initialize() {
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().userId().toString()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));
        colModels.setCellValueFactory(c -> {
            final String models = c.getValue().certifiedAircraftModels().stream()
                    .map(m -> m.modelName()).sorted().collect(Collectors.joining(", "));
            return new SimpleStringProperty(models.isEmpty() ? "—" : models);
        });
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Assign to My Company");
            {
                btn.getStyleClass().add("btn-primary");
                btn.setOnAction(e -> onAssign(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(data);

        detectCompany();
        refresh();
    }

    private void detectCompany() {
        try {
            final String email = AuthzRegistry.authorizationService().session()
                    .map(s -> s.authenticatedUser().username().toString())
                    .orElse(null);
            if (email == null) return;

            PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(UserId.valueOf(email))
                    .ifPresent(collab -> {
                        myCompanyId = collab.airTransportCompanyId();
                        if (myCompanyId != null) {
                            PersistenceContext.repositories().airTransportCompanies()
                                    .ofIdentity(myCompanyId)
                                    .ifPresent(c -> pageTitle.setText(
                                            "Available Pilots — " + c.name() + " (" + c.iataCode() + ")"));
                        }
                    });
        } catch (final Exception ignored) {
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        final List<Pilot> allPilots = new ArrayList<>();
        PersistenceContext.repositories().pilots().findAll().forEach(allPilots::add);

        final Set<String> assignedIds = new HashSet<>();
        assignments().findAll().forEach(a -> assignedIds.add(a.pilotUserId().toString()));

        final List<Pilot> available = allPilots.stream()
                .filter(p -> !assignedIds.contains(p.userId().toString()))
                .toList();

        data.setAll(available);
        statusLabel.setText(available.size() + " pilot(s) not yet assigned to any company");
    }

    private void onAssign(final Pilot pilot) {
        if (myCompanyId == null) {
            statusLabel.setText("Your account is not linked to a company.");
            return;
        }
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Assign pilot \"" + pilot.userId() + "\" to your company?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                assignments().save(new PilotCompanyAssignment(pilot.userId(), myCompanyId));
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }
}
