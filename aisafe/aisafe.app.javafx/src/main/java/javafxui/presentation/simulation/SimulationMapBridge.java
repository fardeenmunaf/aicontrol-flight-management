package javafxui.presentation.simulation;

import javafx.application.Platform;
import netscape.javascript.JSObject;

import java.util.function.Consumer;

public class SimulationMapBridge {

    private final Consumer<String> onPinClickedCallback;

    public SimulationMapBridge(final Consumer<String> onPinClickedCallback) {
        this.onPinClickedCallback = onPinClickedCallback;
    }

    /** Called from JavaScript: bridge.onPinClicked(iata) */
    @SuppressWarnings("unused")
    public void onPinClicked(final String iata) {
        Platform.runLater(() -> onPinClickedCallback.accept(iata));
    }

    public static void install(final javafx.scene.web.WebEngine engine,
                               final Consumer<String> onPinClicked) {
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                final JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("bridge", new SimulationMapBridge(onPinClicked));
            }
        });
    }
}
