package javafxui.presentation.pilot;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import infrastructure.persistence.PersistenceContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafxui.presentation.common.CardListCell;
import javafxui.presentation.common.Cards;
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
    @FXML private ListView<Pilot> table;

    private AirTransportCompanyId myCompanyId;
    private final ObservableList<Pilot> data = FXCollections.observableArrayList();

    private JpaPilotCompanyAssignmentRepository assignments() {
        return new JpaPilotCompanyAssignmentRepository("aisafe-pu");
    }

    @FXML
    public void initialize() {
        table.setCellFactory(lv -> new CardListCell<>(pilot -> {
            final boolean active = pilot.status() == PilotStatus.ACTIVE;
            final HBox header = Cards.row(8,
                    Cards.title(pilot.userId().toString()),
                    Cards.grow(),
                    Cards.chip(pilot.status().name(), active ? "chip-success" : "chip-danger"));

            final String models = pilot.certifiedAircraftModels().stream()
                    .map(m -> m.modelName()).sorted().collect(Collectors.joining(", "));

            final Button toggle = new Button(active ? "Deactivate" : "Activate");
            toggle.getStyleClass().setAll("button", active ? "btn-warning" : "btn-success");
            toggle.setOnAction(e -> onToggleStatus(pilot));
            final Button remove = new Button("Remove");
            remove.getStyleClass().add("btn-danger");
            remove.setOnAction(e -> onRemove(pilot));

            return Cards.card(header,
                    Cards.meta("Certified: " + (models.isEmpty() ? "—" : models)),
                    Cards.row(8, Cards.grow(), toggle, remove));
        }));
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
