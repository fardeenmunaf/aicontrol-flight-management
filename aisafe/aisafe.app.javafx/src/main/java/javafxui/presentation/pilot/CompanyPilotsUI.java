package javafxui.presentation.pilot;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import jpa.JpaPilotCompanyAssignmentRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompanyPilotsUI {

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
            private final Button btnToggle = new Button();
            private final Button btnRemove = new Button("Remove");
            private final HBox box = new HBox(6, btnToggle, btnRemove);
            {
                box.setAlignment(Pos.CENTER);
                btnRemove.getStyleClass().add("btn-danger");
                btnRemove.setOnAction(e -> onRemove(getTableView().getItems().get(getIndex())));
                btnToggle.setOnAction(e -> onToggleStatus(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    final Pilot pilot = getTableView().getItems().get(getIndex());
                    final boolean active = pilot.status() == PilotStatus.ACTIVE;
                    btnToggle.setText(active ? "Deactivate" : "Activate");
                    btnToggle.getStyleClass().setAll("button", active ? "btn-warning" : "btn-success");
                    setGraphic(box);
                }
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
                                            "Pilots of " + c.name() + " (" + c.iataCode() + ")"));
                        }
                    });
        } catch (final Exception ignored) {
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void onToggleStatus(final Pilot pilot) {
        final boolean isActive = pilot.status() == PilotStatus.ACTIVE;
        final String action = isActive ? "deactivate" : "activate";
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to " + action + " pilot \"" + pilot.userId() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                if (isActive) {
                    final boolean hasFlightPlans = PersistenceContext.repositories()
                            .flightPlans()
                            .findByPilotRef(pilot.userId().toString())
                            .iterator().hasNext();
                    if (hasFlightPlans) {
                        new Alert(Alert.AlertType.WARNING,
                                "Cannot deactivate: pilot has flight plans assigned.")
                                .showAndWait();
                        return;
                    }
                }
                final var repo = PersistenceContext.repositories().pilots();
                final var fresh = repo.ofIdentity(pilot.identity())
                        .orElseThrow(() -> new IllegalStateException("Pilot not found"));
                if (isActive) fresh.deactivate(); else fresh.activate();
                repo.save(fresh);
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void onRemove(final Pilot pilot) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove pilot \"" + pilot.userId() + "\" from your company?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                final var repo = assignments();
                repo.ofIdentity(pilot.userId()).ifPresent(repo::delete);
                refresh();
            } catch (final Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void refresh() {
        if (myCompanyId == null) {
            statusLabel.setText("No company linked to this account.");
            return;
        }

        // IDs of pilots assigned to my company
        final Set<String> myPilotIds = assignments().findByCompanyId(myCompanyId).stream()
                .map(a -> a.pilotUserId().toString())
                .collect(Collectors.toSet());

        // Match against Pilot records
        final List<Pilot> allPilots = new ArrayList<>();
        PersistenceContext.repositories().pilots().findAll().forEach(allPilots::add);

        final List<Pilot> companyPilots = allPilots.stream()
                .filter(p -> myPilotIds.contains(p.userId().toString()))
                .toList();

        data.setAll(companyPilots);
        statusLabel.setText(companyPilots.size() + " pilot(s) assigned to your company");
    }
}
