package weatherData.application.importer;

import weatherData.dto.WeatherDataDTO;
import java.util.List;

/**
 * Abstract interface for weather data importers.
 * Allows the system to easily expand to new weather data sources.
 *
 * Implementations should handle:
 * - Parsing data from specific formats/sources
 * - Validating the input data
 * - Converting to WeatherDataDTO objects
 */
public interface WeatherDataImporter {

    /**
     * Import weather data from a specific source.
     *
     * @return a list of WeatherDataDTO objects parsed from the source
     * @throws ImportException if there are errors during import
     */
    List<WeatherDataDTO> importData() throws ImportException;

    /**
     * Get the name/identifier of this importer.
     * Used for logging and identifying the data source provider.
     *
     * @return the importer's identifier
     */
    String getProviderName();

    /**
     * Exception wrapper for weather data import errors
     */
    class ImportException extends Exception {
        private final int lineNumber;

        public ImportException(final String message, final int lineNumber) {
            super(String.format("Line %d: %s", lineNumber, message));
            this.lineNumber = lineNumber;
        }

        public ImportException(final String message, final int lineNumber, final Throwable cause) {
            super(String.format("Line %d: %s", lineNumber, message), cause);
            this.lineNumber = lineNumber;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }
}

