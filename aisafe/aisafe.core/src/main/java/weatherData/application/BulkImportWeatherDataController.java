package weatherData.application;

import infrastructure.persistence.PersistenceContext;
import weatherData.application.importer.WeatherDataImporter;
import weatherData.dto.WeatherDataDTO;
import weatherData.repository.WeatherDataRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * BulkImportWeatherDataController
 * Orchestrates the bulk import of weather data from multiple providers.
 *
 * Responsibilities:
 * - Accept a WeatherDataImporter implementation
 * - Execute the import process
 * - Validate and register individual weather data entries
 * - Report import results (success/error counts)
 */
public class BulkImportWeatherDataController {

    private final WeatherDataRepository repo = PersistenceContext.repositories().weatherData();
    private final RegisterWeatherDataController registerController = new RegisterWeatherDataController();

    /**
     * Import bulk weather data using the provided importer.
     *
     * @param importer the WeatherDataImporter implementation to use
     * @return an ImportResult containing statistics about the import operation
     */
    public ImportResult importBulkWeatherData(final WeatherDataImporter importer) {
        final ImportResult result = new ImportResult(importer.getProviderName());

        try {
            final List<WeatherDataDTO> weatherDataList = importer.importData();

            for (final WeatherDataDTO dto : weatherDataList) {
                try {
                    registerController.registerWeatherData(dto);
                    result.incrementSuccessCount();
                } catch (final IllegalArgumentException ex) {
                    result.addError(ex.getMessage());
                    result.incrementErrorCount();
                }
            }

            result.setCompleted(true);

        } catch (final WeatherDataImporter.ImportException ex) {
            result.addError("Import failed at line " + ex.getLineNumber() + ": " + ex.getMessage());
            result.setCompleted(false);
        }

        return result;
    }

    /**
     * Result object containing statistics about the import operation
     */
    public static class ImportResult {
        private final String providerName;
        private int successCount = 0;
        private int errorCount = 0;
        private boolean completed = false;
        private final List<String> errors = new ArrayList<>();

        public ImportResult(final String providerName) {
            this.providerName = providerName;
        }

        public void incrementSuccessCount() {
            this.successCount++;
        }

        public void incrementErrorCount() {
            this.errorCount++;
        }

        public void addError(final String error) {
            this.errors.add(error);
        }

        public void setCompleted(final boolean completed) {
            this.completed = completed;
        }

        public String getProviderName() {
            return providerName;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public boolean isCompleted() {
            return completed;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public int getTotalProcessed() {
            return successCount + errorCount;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("=== Import Result ===\n");
            sb.append("Provider: ").append(providerName).append("\n");
            sb.append("Total Processed: ").append(getTotalProcessed()).append("\n");
            sb.append("Successful: ").append(successCount).append("\n");
            sb.append("Errors: ").append(errorCount).append("\n");

            if (!errors.isEmpty()) {
                sb.append("\nError Details:\n");
                for (int i = 0; i < errors.size(); i++) {
                    sb.append((i + 1)).append(". ").append(errors.get(i)).append("\n");
                }
            }

            sb.append("Completed: ").append(completed ? "Yes" : "No").append("\n");

            return sb.toString();
        }
    }
}

