package javafxui.presentation.flightroute;

import airport.domain.Airport;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import flightRoute.application.CreateFlightRouteController;
import flightRoute.application.DeleteFlightRouteController;
import flightRoute.domain.FlightRoute;
import infrastructure.persistence.PersistenceContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafxui.presentation.common.CardListCell;
import javafxui.presentation.common.Cards;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class FlightRoutesUI {

    @FXML
    private ListView<FlightRoute> routesTable;
    @FXML
    private Label messageLabel;
    @FXML
    private VBox createForm;
    @FXML
    private Label lblPrefix;
    @FXML
    private TextField fSuffix;
    @FXML
    private DatePicker dpActiveFrom;
    @FXML
    private ComboBox<Airport> cbOrigin, cbDestination;
    @FXML
    private Label formError;

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
        final StringConverter<Airport> airportConverter = new StringConverter<>() {
            @Override
            public String toString(final Airport a) {
                return a == null ? "" : a.iataCode() + " — " + a.name() + " (" + a.town() + ")";
            }

            @Override
            public Airport fromString(final String s) {
                return null;
            }
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
        } catch (final Exception ignored) {
        }
        routesTable.setCellFactory(lv -> new CardListCell<>(route -> {
            final boolean active = route.activeUntil() == null;
            final HBox header = Cards.row(8,
                    Cards.title(route.designation().value()),
                    Cards.chip(route.originAirport().iataCode() + " → "
                            + route.destinationAirport().iataCode(), "chip-neutral"),
                    Cards.grow(),
                    Cards.chip(active ? "Active" : "Inactive from " + route.activeUntil(),
                            active ? "chip-success" : "chip-danger"));

            final Button actionBtn;
            if (active) {
                actionBtn = new Button("Deactivate");
                actionBtn.getStyleClass().add("btn-danger");
                actionBtn.setOnAction(e -> onDeactivate(route));
            } else {
                actionBtn = new Button("Reactivate");
                actionBtn.getStyleClass().add("btn-success");
                actionBtn.setOnAction(e -> onReactivate(route));
            }

            return Cards.card(header,
                    Cards.meta("Active from " + route.activeFrom()),
                    Cards.row(8, Cards.grow(), actionBtn));
        }));
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
        // Find the latest future flight departure for this route
        final Instant nowInstant = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        final Optional<Instant> latestDeparture = StreamSupport.stream(
                PersistenceContext.repositories().flightPlans()
                        .findByRouteIdAndDepartureAfter(route.designation().value(), nowInstant)
                        .spliterator(),
                false)
                .map(fp -> fp.departureTime())
                .max(Comparator.naturalOrder());

        // Minimum valid deactivation date = day after the latest flight (so no future
        // flights remain after cutoff)
        final LocalDate minFromFlights = latestDeparture
                .map(d -> d.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1))
                .orElse(LocalDate.now());

        final LocalDate minDate = List.of(LocalDate.now(), route.activeFrom(), minFromFlights)
                .stream().max(Comparator.naturalOrder()).orElse(LocalDate.now());

        final DatePicker datePicker = new DatePicker(minDate);
        datePicker.setShowWeekNumbers(false);
        datePicker.setMaxWidth(Double.MAX_VALUE);

        final Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(final LocalDate item, final boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(minDate)) {
                    setDisable(true);
                    setStyle("-fx-background-color:#0f1520;-fx-text-fill:#5b6573;");
                }
            }
        };
        datePicker.setDayCellFactory(dayCellFactory);

        final String hintText = latestDeparture.isPresent()
                ? "Earliest selectable date: " + minDate + " (route has flights until "
                        + latestDeparture.get().atZone(ZoneOffset.UTC).toLocalDate() + ")"
                : "Earliest selectable date: " + minDate;
        final Label hint = new Label(hintText);
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill:#7dd3fc;-fx-font-size:11px;");

        final VBox content = new VBox(8, datePicker, hint);
        content.setPrefWidth(280);

        final Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Deactivate Flight Route");
        dialog.setHeaderText("Route: " + route.designation().value()
                + "  [" + route.originAirport().iataCode()
                + " → " + route.destinationAirport().iataCode() + "]\n"
                + "Select the last active date:");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(datePicker.getValue() == null);
        datePicker.valueProperty().addListener((obs, o, n) -> okBtn.setDisable(n == null || n.isBefore(minDate)));

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? datePicker.getValue() : null);

        dialog.showAndWait().ifPresent(deactivationDate -> {
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
        });
    }
}
