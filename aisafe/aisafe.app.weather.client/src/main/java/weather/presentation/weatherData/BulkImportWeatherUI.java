package weather.presentation.weatherData;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import weatherData.application.BulkImportWeatherDataController;
import weatherData.application.importer.CSVWeatherDataImporter;
import weatherData.application.importer.WeatherDataImporter;
import weatherData.dto.WeatherDataDTO;

/**
 * UI for bulk importing weather data from CSV files.
 *
 * CSV Format (12 fields, semicolon-delimited):
 * ACA;Lat1;Long1;Lat2;Long2;Alt_inf;Alt_sup;Direction;Value;Date;Start_hour;End_hour
 *
 * Example:
 * SCPA;38.5;-9.0;39.0;-8.5;0;10000;180;15.5;2024-01-15;08:00;18:00
 */
public class BulkImportWeatherUI extends AbstractUI {

    private final BulkImportWeatherDataController controller = new BulkImportWeatherDataController();

    @Override
    protected boolean doShow() {
        System.out.println("\n=== Weather Data Bulk Import ===");
        System.out.println("Expected CSV Format (semicolon-delimited, 12 fields):");
        System.out.println("ACA;Lat1;Long1;Lat2;Long2;Alt_inf;Alt_sup;Direction;Value;Date;Start_hour;End_hour");
        System.out.println("Example: 121;43.84;-9.79;40.22;-7.95;0;1000;90;28.75;22/06/2026;05:00;08:15");
        System.out.println();

        final String filePath = Console.readLine("CSV file path");

        try {
            // Create importer for CSV files
            final WeatherDataImporter importer = new CSVWeatherDataImporter(filePath);

            if (weather.ClientSession.getMode() == weather.ClientSession.ConnectionMode.DIRECT_DB) {
                // Execute bulk import localmente
                final BulkImportWeatherDataController.ImportResult result = controller.importBulkWeatherData(importer);
                System.out.println("\n" + result.toString());
                return result.isCompleted();
            } else {
                // TCP Mode - Enviar lista como JSON
                final Iterable<WeatherDataDTO> dtos = importer.importData();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String jsonPayload = mapper.writeValueAsString(dtos);

                String response = weather.ClientSession.getClient().uploadWeather(jsonPayload);
                System.out.println("\nServer response: " + response);
                return true;
            }

        } catch (final Exception ex) {
            System.out.println("Unexpected error during import: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Bulk Import Weather Data";
    }
}
