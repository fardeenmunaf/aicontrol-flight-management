package javafxui.presentation.airports;

import airport.application.ListAirportsController;
import airport.domain.Airport;
import infrastructure.persistence.PersistenceContext;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafxui.presentation.simulation.SimulationMapBridge;

import java.util.Locale;

public class AirportsMapUI {

    @FXML private WebView mapView;
    @FXML private Label statusLabel;

    private final ListAirportsController controller = new ListAirportsController(
            PersistenceContext.repositories().airports());

    @FXML
    public void initialize() {
        final WebEngine engine = mapView.getEngine();
        final var mapResource = getClass().getResource("/javafxui/map.html");
        if (mapResource == null) {
            statusLabel.setText("Error: map.html not found");
            return;
        }
        engine.load(mapResource.toExternalForm());

        // Both listeners registered here fire once on the same WebEngine instance (fresh per FXML load).
        // If this FXML is ever cached/reused, bridge and pins would accumulate — keep FXML un-cached.
        SimulationMapBridge.install(engine, iata -> {});

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> loadAirportPins(engine));
            }
        });
    }

    private void loadAirportPins(final WebEngine engine) {
        final Iterable<Airport> airports = controller.listAirports();
        int count = 0;
        for (final Airport airport : airports) {
            final String iata = airport.iataCode();
            final String name = airport.name().replace("'", "\\'");
            final double lat  = airport.coordinates().latitude();
            final double lon  = airport.coordinates().longitude();
            engine.executeScript(String.format(Locale.ROOT,
                    "addPin('%s', '%s', %f, %f)", iata, name, lat, lon));
            count++;
        }
        engine.executeScript("fitAirports()");
        final int finalCount = count;
        Platform.runLater(() -> statusLabel.setText(finalCount + " airports loaded"));
    }
}
