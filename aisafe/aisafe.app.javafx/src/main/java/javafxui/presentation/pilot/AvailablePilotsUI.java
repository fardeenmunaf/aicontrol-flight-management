package javafxui.presentation.pilot;

import infrastructure.persistence.PersistenceContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pilot.domain.Pilot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AvailablePilotsUI {

    @FXML private TableView<Pilot> table;
    @FXML private TableColumn<Pilot, String> colEmail;
    @FXML private TableColumn<Pilot, String> colStatus;
    @FXML private TableColumn<Pilot, String> colModels;
    @FXML private Label statusLabel;

    private final ObservableList<Pilot> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().userId().toString()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));
        colModels.setCellValueFactory(c -> {
            final String models = c.getValue().certifiedAircraftModels().stream()
                    .map(m -> m.modelName())
                    .sorted()
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(models.isEmpty() ? "—" : models);
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(data);
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        final List<Pilot> list = new ArrayList<>();
        PersistenceContext.repositories().pilots().findAll().forEach(list::add);
        data.setAll(list);
        statusLabel.setText(list.size() + " pilot(s) available");
    }
}
