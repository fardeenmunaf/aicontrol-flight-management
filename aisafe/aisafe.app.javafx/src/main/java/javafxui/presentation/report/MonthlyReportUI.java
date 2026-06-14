package javafxui.presentation.report;

import infrastructure.persistence.PersistenceContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import report.monthly.GenerateMonthlyStatisticsReportController;
import report.monthly.MonthlyStatisticsCollector;

import java.nio.file.Path;

/** Lets an authenticated Flight Control Operator generate the monthly statistics PDF. */
public class MonthlyReportUI {

    private static final Path OUTPUT_DIR = Path.of("reports");

    @FXML private TextField yearField;
    @FXML private TextField monthField;
    @FXML private Label resultLabel;

    @FXML
    public void onGenerate() {
        Integer year = null;
        Integer month = null;
        try {
            if (!yearField.getText().isBlank()) {
                year = Integer.parseInt(yearField.getText().trim());
            }
            if (!monthField.getText().isBlank()) {
                month = Integer.parseInt(monthField.getText().trim());
            }
        } catch (final NumberFormatException e) {
            resultLabel.setText("Year and month must be whole numbers.");
            return;
        }

        // Built lazily (on click) so opening the screen never depends on the DB.
        final GenerateMonthlyStatisticsReportController controller =
                new GenerateMonthlyStatisticsReportController(
                        new MonthlyStatisticsCollector(PersistenceContext.repositories().simulations()),
                        OUTPUT_DIR);
        try {
            final Path file = controller.generate(year, month);
            resultLabel.setText("Report generated: " + file.toAbsolutePath());
        } catch (final IllegalArgumentException e) {
            resultLabel.setText("Error: " + e.getMessage());
        } catch (final RuntimeException e) {
            resultLabel.setText("Failed to generate report: " + e.getMessage());
        }
    }
}
