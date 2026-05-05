package weather.presentation;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import weatherData.application.RegisterWeatherDataController;

public class BulkImportWeatherUI extends AbstractUI {

    private final RegisterWeatherDataController controller = new RegisterWeatherDataController();

    @Override
    protected boolean doShow() {
        final String filePath = Console.readLine("CSV file path");
        int successCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    final String[] parts = line.split(";");
                    if (parts.length < 6) {
                        throw new IllegalArgumentException("Expected 6 fields, got " + parts.length);
                    }

                    final String airControlArea = parts[0].trim();
                    final LocalDateTime dateTime = LocalDateTime.parse(parts[1].trim());
                    final Double windDirection = Double.parseDouble(parts[2].trim());
                    final Double windSpeed = Double.parseDouble(parts[3].trim());
                    final Double temperature = Double.parseDouble(parts[4].trim());
                    final Double pressure = Double.parseDouble(parts[5].trim());

                    controller.registerWeatherData(
                            airControlArea,
                            dateTime,
                            windDirection,
                            windSpeed,
                            temperature,
                            pressure
                    );
                    successCount++;
                } catch (final IllegalArgumentException | DateTimeParseException ex) {
                    errorCount++;
                    System.out.printf("Line %d skipped: %s%n", lineNumber, ex.getMessage());
                }
            }
        } catch (final IOException ex) {
            System.out.println("Error reading file: " + ex.getMessage());
            return false;
        }

        System.out.printf("Bulk import finished. Success: %d, Errors: %d%n", successCount, errorCount);
        return true;
    }

    @Override
    public String headline() {
        return "Bulk Import Weather Data";
    }
}

