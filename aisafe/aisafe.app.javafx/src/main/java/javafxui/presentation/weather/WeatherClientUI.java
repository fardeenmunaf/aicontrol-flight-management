package javafxui.presentation.weather;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import weatherData.application.BulkImportWeatherDataController;
import weatherData.application.ListWeatherDataController;
import weatherData.application.RegisterWeatherDataController;
import weatherData.application.importer.CSVWeatherDataImporter;
import weatherData.dto.WeatherDataDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class WeatherClientUI {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String EMPTY_MENU_MESSAGE = "Select an option from the Weather Client menu.";

    @FXML private Label statusLabel;
    @FXML private Label placeholderLabel;

    @FXML private VBox registerPane;
    @FXML private TextField fAcaCode;
    @FXML private DatePicker fDate;
    @FXML private TextField fLat1;
    @FXML private TextField fLon1;
    @FXML private TextField fLat2;
    @FXML private TextField fLon2;
    @FXML private TextField fAltInf;
    @FXML private TextField fAltSup;
    @FXML private TextField fDirection;
    @FXML private TextField fWindSpeed;
    @FXML private TextField fStartHour;
    @FXML private TextField fEndHour;
    @FXML private Label registerError;

    @FXML private VBox viewPane;
    @FXML private TextField filterArea;
    @FXML private DatePicker filterDate;
    @FXML private TableView<WeatherDataDTO> table;
    @FXML private TableColumn<WeatherDataDTO, String> colAca;
    @FXML private TableColumn<WeatherDataDTO, String> colLat1;
    @FXML private TableColumn<WeatherDataDTO, String> colLon1;
    @FXML private TableColumn<WeatherDataDTO, String> colLat2;
    @FXML private TableColumn<WeatherDataDTO, String> colLon2;
    @FXML private TableColumn<WeatherDataDTO, String> colAltInf;
    @FXML private TableColumn<WeatherDataDTO, String> colAltSup;
    @FXML private TableColumn<WeatherDataDTO, String> colDirection;
    @FXML private TableColumn<WeatherDataDTO, String> colWindSpeed;
    @FXML private TableColumn<WeatherDataDTO, String> colDate;
    @FXML private TableColumn<WeatherDataDTO, String> colStart;
    @FXML private TableColumn<WeatherDataDTO, String> colEnd;
    @FXML private Label viewStatus;

    @FXML private VBox importPane;
    @FXML private TextField importPath;
    @FXML private TextArea importResult;
    @FXML private Label importError;

    private final RegisterWeatherDataController registerCtrl = new RegisterWeatherDataController();
    private final ListWeatherDataController listCtrl = new ListWeatherDataController();
    private final BulkImportWeatherDataController bulkCtrl = new BulkImportWeatherDataController();
    private final ObservableList<WeatherDataDTO> viewData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colAca.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().acaCode())));
        colLat1.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().latitude1(), "%.6f")));
        colLon1.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().longitude1(), "%.6f")));
        colLat2.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().latitude2(), "%.6f")));
        colLon2.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().longitude2(), "%.6f")));
        colAltInf.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().altitudeInf(), "%.0f")));
        colAltSup.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().altitudeSup(), "%.0f")));
        colDirection.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().direction(), "%.1f")));
        colWindSpeed.setCellValueFactory(c -> new SimpleStringProperty(formatDouble(c.getValue().windSpeed(), "%.1f")));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().date())));
        colStart.setCellValueFactory(c -> new SimpleStringProperty(formatTime(c.getValue().startHour())));
        colEnd.setCellValueFactory(c -> new SimpleStringProperty(formatTime(c.getValue().endHour())));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(viewData);
        showPlaceholder(EMPTY_MENU_MESSAGE);
    }

    @FXML
    private void showRegister() {
        registerError.setText("");
        showPane(registerPane, "Register Weather Data");
    }

    @FXML
    private void showView() {
        showPane(viewPane, "View Weather Data");
        loadAllWeatherData();
    }

    @FXML
    private void showBulkImport() {
        importError.setText("");
        showPane(importPane, "Bulk Import Weather Data");
    }

    @FXML
    private void onExit() {
        showPlaceholder("Weather client closed.");
    }

    @FXML
    private void onSaveRegister() {
        registerError.setText("");
        try {
            final String acaCode = readRequiredText(fAcaCode, "ACA code");
            final LocalDate date = requireDate(fDate, "Date");
            final double lat1 = parseDouble(fLat1, "Latitude 1");
            final double lon1 = parseDouble(fLon1, "Longitude 1");
            final double lat2 = parseDouble(fLat2, "Latitude 2");
            final double lon2 = parseDouble(fLon2, "Longitude 2");
            final double altInf = parseDouble(fAltInf, "Altitude inferior");
            final double altSup = parseDouble(fAltSup, "Altitude superior");
            final double direction = parseDouble(fDirection, "Wind direction");
            final double windSpeed = parseDouble(fWindSpeed, "Wind speed");
            final LocalTime start = parseTime(fStartHour, "Start hour");
            final LocalTime end = parseTime(fEndHour, "End hour");

            final WeatherDataDTO dto = new WeatherDataDTO(
                    acaCode, lat1, lon1, lat2, lon2, altInf, altSup,
                    direction, windSpeed, date, start, end);
            registerCtrl.registerWeatherData(dto);
            statusLabel.setText("Weather data registered successfully.");
            clearRegisterForm();
            showPlaceholder(EMPTY_MENU_MESSAGE);
        } catch (final IllegalArgumentException e) {
            registerError.setText(e.getMessage());
        }
    }

    @FXML
    private void onCancelRegister() {
        clearRegisterForm();
        showPlaceholder(EMPTY_MENU_MESSAGE);
    }

    @FXML
    private void onApplyFilter() {
        final String area = filterArea.getText().trim();
        final LocalDate date = filterDate.getValue();
        final Iterable<WeatherDataDTO> records = (area.isEmpty() && date == null)
                ? listCtrl.listAll()
                : listCtrl.listByDateAndArea(area.isEmpty() ? null : area, date);
        refreshView(records);
    }

    @FXML
    private void onClearFilter() {
        filterArea.clear();
        filterDate.setValue(null);
        loadAllWeatherData();
    }

    @FXML
    private void onImport() {
        importError.setText("");
        final String path = importPath.getText().trim();
        if (path.isEmpty()) {
            importError.setText("CSV file path is required.");
            return;
        }
        try {
            final var importer = new CSVWeatherDataImporter(path);
            final var result = bulkCtrl.importBulkWeatherData(importer);
            importResult.setText(result.toString());
            statusLabel.setText("Bulk import completed: " + result.getSuccessCount()
                    + " ok, " + result.getErrorCount() + " errors.");
            if (!result.isCompleted()) {
                importError.setText("Import finished with errors.");
            }
        } catch (final IllegalArgumentException e) {
            importResult.setText("");
            importError.setText("Import failed: " + e.getMessage());
        }
    }

    private void showPane(final VBox pane, final String status) {
        setPaneVisible(registerPane, false);
        setPaneVisible(viewPane, false);
        setPaneVisible(importPane, false);
        setPaneVisible(placeholderLabel, false);
        setPaneVisible(pane, true);
        statusLabel.setText(status);
    }

    private void showPlaceholder(final String status) {
        setPaneVisible(registerPane, false);
        setPaneVisible(viewPane, false);
        setPaneVisible(importPane, false);
        setPaneVisible(placeholderLabel, true);
        placeholderLabel.setText(status);
        statusLabel.setText(status);
    }

    private void setPaneVisible(final VBox pane, final boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void setPaneVisible(final Label label, final boolean visible) {
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void loadAllWeatherData() {
        refreshView(listCtrl.listAll());
    }

    private void refreshView(final Iterable<WeatherDataDTO> records) {
        final List<WeatherDataDTO> list = new ArrayList<>();
        records.forEach(list::add);
        viewData.setAll(list);
        viewStatus.setText(list.isEmpty() ? "No records found." : list.size() + " records");
    }

    private String readRequiredText(final TextField field, final String label) {
        final String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    private LocalDate requireDate(final DatePicker picker, final String label) {
        final LocalDate date = picker.getValue();
        if (date == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return date;
    }

    private double parseDouble(final TextField field, final String label) {
        final String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
    }

    private LocalTime parseTime(final TextField field, final String label) {
        final String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (final DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " must be in HH:mm format.");
        }
    }

    private void clearRegisterForm() {
        fAcaCode.clear();
        fDate.setValue(null);
        fLat1.clear();
        fLon1.clear();
        fLat2.clear();
        fLon2.clear();
        fAltInf.clear();
        fAltSup.clear();
        fDirection.clear();
        fWindSpeed.clear();
        fStartHour.clear();
        fEndHour.clear();
        registerError.setText("");
    }

    private String formatDouble(final Double value, final String pattern) {
        if (value == null) return "—";
        return String.format(pattern, value);
    }

    private String formatDate(final LocalDate value) {
        return value == null ? "—" : value.toString();
    }

    private String formatTime(final LocalTime value) {
        return value == null ? "—" : value.toString();
    }

    private String safe(final String value) {
        return value == null ? "—" : value;
    }
}
