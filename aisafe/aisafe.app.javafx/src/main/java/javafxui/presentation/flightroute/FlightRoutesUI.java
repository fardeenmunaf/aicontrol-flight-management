package javafxui.presentation.flightroute;

import airport.domain.Airport;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightRoute.application.CreateFlightRouteController;
import flightRoute.application.DeleteFlightRouteController;
import flightRoute.domain.FlightRoute;
import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlightRoutesUI {

    @FXML private TableView<FlightRoute> routesTable;
    @FXML private TableColumn<FlightRoute, String> colDesignation;
    @FXML private TableColumn<FlightRoute, String> colOrigin;
    @FXML private TableColumn<FlightRoute, String> colDestination;
    @FXML private TableColumn<FlightRoute, String> colActiveFrom;
    @FXML private TableColumn<FlightRoute, String> colStatus;
    @FXML private TableColumn<FlightRoute, String> colAction;
    @FXML private Label messageLabel;
    @FXML private VBox createForm;
    @FXML private Label lblPrefix;
    @FXML private TextField fSuffix;
    @FXML private DatePicker dpActiveFrom;
    @FXML private ComboBox<Airport> cbOrigin, cbDestination;
    @FXML private Label formError;

    private String companyIataPrefix = "";
    private AirTransportCompanyId companyId = null;

    private final CreateFlightRouteController createCtrl = new CreateFlightRouteController(
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().airports(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies());

    private final DeleteFlightRouteController deleteCtrl = new DeleteFlightRouteController(
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().flightPlans(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies());

    private final ObservableList<FlightRoute> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDesignation.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().designation().value()));
        colOrigin.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().originAirport().iataCode()));
        colDestination.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().destinationAirport().iataCode()));
        colActiveFrom.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().activeFrom().toString()));
        colStatus.setCellValueFactory(c -> {
            final FlightRoute r = c.getValue();
            final String status = r.activeUntil() == null
                    ? "Active"
                    : "Inactive from " + r.activeUntil();
            return new SimpleStringProperty(status);
        });

        final StringConverter<Airport> airportConverter = new StringConverter<>() {
            @Override public String toString(final Airport a) {
                return a == null ? "" : a.iataCode() + " — " + a.name() + " (" + a.town() + ")";
            }
            @Override public Airport fromString(final String s) { return null; }
        };
        cbOrigin.setConverter(airportConverter);
        cbDestination.setConverter(airportConverter);

        final List<Airport> airports = new ArrayList<>();
        PersistenceContext.repositories().airports().findAll().forEach(airports::add);
        airports.sort((a, b) -> a.iataCode().compareTo(b.iataCode()));
        cbOrigin.setItems(FXCollections.observableArrayList(airports));
        cbDestination.setItems(FXCollections.observableArrayList(airports));

        try {
            final var session = AuthzRegistry.authorizationService().session().orElse(null);
            if (session != null) {
                final UserId userId = UserId.valueOf(session.authenticatedUser().identity().toString());
                final CustomerCollaborator collaborator = PersistenceContext.repositories()
                        .customerCollaborators().findByUserId(userId).orElse(null);
                if (collaborator != null && collaborator.customerType() == CustomerType.AIR_TRANSPORT_COMPANY) {
                    companyId = collaborator.airTransportCompanyId();
                    PersistenceContext.repositories().airTransportCompanies()
                            .ofIdentity(companyId)
                            .ifPresent(c -> companyIataPrefix = c.iataCode());
                }
            }
        } catch (final Exception ignored) {}
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button deactivateBtn  = new Button("Deactivate");
            private final Button reactivateBtn  = new Button("Reactivate");
            {
                deactivateBtn.getStyleClass().add("btn-danger");
                reactivateBtn.getStyleClass().add("btn-success");
            }

            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                final FlightRoute route = getTableView().getItems().get(getIndex());
                if (route.activeUntil() == null) {
                    deactivateBtn.setOnAction(e -> onDeactivate(route));
                    setGraphic(deactivateBtn);
                } else {
                    reactivateBtn.setOnAction(e -> onReactivate(route));
                    setGraphic(reactivateBtn);
                }
            }
        });

        routesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        routesTable.setItems(data);
        refreshTable();
    }

    private void refreshTable() {
        final List<FlightRoute> list = new ArrayList<>();
        if (companyId != null) {
            PersistenceContext.repositories().flightRoutes().findByCompanyId(companyId).forEach(list::add);
        } else {
            PersistenceContext.repositories().flightRoutes().findAll().forEach(list::add);
        }
        data.setAll(list);
        routesTable.refresh();
        messageLabel.setText(list.size() + " route(s)");
    }

    @FXML
    private void onRefresh() {
        refreshTable();
        messageLabel.setText("Refreshed — " + data.size() + " route(s)");
    }

    @FXML
    private void onCreateRoute() {
        lblPrefix.setText(companyIataPrefix);
        fSuffix.clear();
        cbOrigin.getSelectionModel().clearSelection();
        cbDestination.getSelectionModel().clearSelection();
        dpActiveFrom.setValue(LocalDate.now());
        formError.setText("");
        createForm.setVisible(true);
        createForm.setManaged(true);
    }

    @FXML
    private void onCancelCreate() {
        createForm.setVisible(false);
        createForm.setManaged(false);
    }

    @FXML
    private void onSaveCreate() {
        try {
            final Airport origin = cbOrigin.getSelectionModel().getSelectedItem();
            final Airport destination = cbDestination.getSelectionModel().getSelectedItem();

            if (origin == null) {
                formError.setText("Please select an origin airport.");
                return;
            }
            if (destination == null) {
                formError.setText("Please select a destination airport.");
                return;
            }

            final String designation = companyIataPrefix + fSuffix.getText().trim();
            final LocalDate activeFrom = dpActiveFrom.getValue() != null
                    ? dpActiveFrom.getValue()
                    : LocalDate.now();

            createCtrl.createFlightRoute(
                    designation,
                    origin.iataCode(),
                    destination.iataCode(),
                    activeFrom);

            createForm.setVisible(false);
            createForm.setManaged(false);
            refreshTable();
            messageLabel.setText("Route created successfully.");
        } catch (final Exception e) {
            formError.setText(e.getMessage());
        }
    }

    private void onReactivate(final FlightRoute route) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reactivate route " + route.designation().value()
                        + "  [" + route.originAirport().iataCode()
                        + " → " + route.destinationAirport().iataCode() + "]?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Reactivation");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                final var repo = PersistenceContext.repositories().flightRoutes();
                final FlightRoute managed = repo.ofIdentity(route.identity())
                        .orElseThrow(() -> new IllegalStateException("Route not found"));
                managed.reactivate();
                repo.save(managed);
                refreshTable();
                messageLabel.setText("Route " + route.designation().value() + " reactivated.");
            } catch (final Exception e) {
                messageLabel.setText("Error: " + e.getMessage());
            }
        });
    }

    private void onDeactivate(final FlightRoute route) {
        final TextInputDialog dialog = new TextInputDialog(LocalDate.now().toString());
        dialog.setTitle("Deactivate Flight Route");
        dialog.setHeaderText("Deactivate route " + route.designation().value()
                + "  [" + route.originAirport().iataCode()
                + " → " + route.destinationAirport().iataCode() + "]");
        dialog.setContentText("Last active date (YYYY-MM-DD):");

        final Optional<String> result = dialog.showAndWait();
        result.ifPresent(dateStr -> {
            try {
                final LocalDate deactivationDate = LocalDate.parse(dateStr.trim());
                final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Deactivate route " + route.designation().value()
                                + " from " + deactivationDate + "?\n"
                                + "No new flights can be created after this date.",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Confirm Deactivation");
                confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                    try {
                        deleteCtrl.deleteFlightRoute(route.identity().toString(), deactivationDate);
                        refreshTable();
                        messageLabel.setText("Route " + route.designation().value()
                                + " deactivated. Last active day: " + deactivationDate);
                    } catch (final Exception e) {
                        messageLabel.setText("Error: " + e.getMessage());
                    }
                });
            } catch (final Exception e) {
                messageLabel.setText("Error: " + e.getMessage());
            }
        });
    }
}
