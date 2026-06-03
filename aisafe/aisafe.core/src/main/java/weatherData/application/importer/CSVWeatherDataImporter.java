package weatherData.application.importer;

import weatherData.dto.WeatherDataDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV Weather Data Importer
 * Reads weather data from CSV files with the following format:
 * ACA;Lat1;Long1;Lat2;Long2;Alt_inf;Alt_sup;Direction;Value;Date;Start_hour;End_hour
 *
 * Where:
 * - ACA: Air Control Area code (e.g., 121)
 * - Lat1, Long1: First coordinate pair
 * - Lat2, Long2: Second coordinate pair
 * - Alt_inf: Altitude inferior in feet
 * - Alt_sup: Altitude superior in feet
 * - Direction: Wind direction in degrees (0-360)
 * - Value: Wind speed in knots
 * - Date: Date in format dd/MM/yyyy (e.g., 22/06/2026)
 * - Start_hour: Start time in format HH:mm (e.g., 05:00)
 * - End_hour: End time in format HH:mm (e.g., 08:15)
 *
 * Example:
 * 121;43,840454;-9,795711;40,225;-7,9501;0;1000;90;28,75;22/06/2026;05:00;08:15
 */
public class CSVWeatherDataImporter implements WeatherDataImporter {

    private final Path filePath;
    private static final String DELIMITER = ";";
    private static final int EXPECTED_FIELDS = 12;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public CSVWeatherDataImporter(final String filePath) {
        this.filePath = Path.of(filePath);
    }

    @Override
    public List<WeatherDataDTO> importData() throws WeatherDataImporter.ImportException {
        final List<WeatherDataDTO> weatherDataList = new ArrayList<>();

        if (!Files.exists(filePath)) {
            throw new WeatherDataImporter.ImportException("File not found: " + filePath, 0);
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNumber = 0;

            // Skip header line if present
            final String firstLine = reader.readLine();
            if (firstLine != null && firstLine.contains("ACA")) {
                lineNumber = 1; // header was line 1
            } else {
                // Process first line as data
                lineNumber = 1;
                if (firstLine != null) {
                    parseAndAddWeatherData(firstLine, lineNumber, weatherDataList);
                }
            }

            // Process remaining lines
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                parseAndAddWeatherData(line, lineNumber, weatherDataList);
            }

        } catch (final IOException ex) {
            throw new WeatherDataImporter.ImportException("Error reading file: " + ex.getMessage(), 0, ex);
        }

        return weatherDataList;
    }

    private void parseAndAddWeatherData(
            final String line,
            final int lineNumber,
            final List<WeatherDataDTO> weatherDataList) throws WeatherDataImporter.ImportException {

        try {
            final String[] parts = line.split(DELIMITER);

            if (parts.length != EXPECTED_FIELDS) {
                throw new WeatherDataImporter.ImportException(
                    "Expected " + EXPECTED_FIELDS + " fields, but got " + parts.length,
                    lineNumber);
            }

            // Parse fields
            final String acaCode = parts[0].trim();
            final Double latitude1 = parseDouble(parts[1], "Latitude 1", lineNumber);
            final Double longitude1 = parseDouble(parts[2], "Longitude 1", lineNumber);
            final Double latitude2 = parseDouble(parts[3], "Latitude 2", lineNumber);
            final Double longitude2 = parseDouble(parts[4], "Longitude 2", lineNumber);
            final Double altitudeInf = parseDouble(parts[5], "Altitude inferior", lineNumber);
            final Double altitudeSup = parseDouble(parts[6], "Altitude superior", lineNumber);
            final Double direction = parseDouble(parts[7], "Direction", lineNumber);
            final Double windSpeed = parseDouble(parts[8], "Wind speed", lineNumber);
            final LocalDate date = parseDate(parts[9], lineNumber);
            final LocalTime startHour = parseTime(parts[10], lineNumber);
            final LocalTime endHour = parseTime(parts[11], lineNumber);

            // Create DTO (validation happens in domain)
            final WeatherDataDTO dto = new WeatherDataDTO(
                acaCode,
                latitude1,
                longitude1,
                latitude2,
                longitude2,
                altitudeInf,
                altitudeSup,
                direction,
                windSpeed,
                date,
                startHour,
                endHour
            );

            weatherDataList.add(dto);

        } catch (final WeatherDataImporter.ImportException ex) {
            throw ex;
        } catch (final NumberFormatException | DateTimeParseException ex) {
            throw new WeatherDataImporter.ImportException(
                "Invalid data format: " + ex.getMessage(),
                lineNumber,
                ex);
        }
    }

    private Double parseDouble(final String value, final String fieldName, final int lineNumber)
            throws WeatherDataImporter.ImportException {
        try {
            final String normalized = value.trim().replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (final NumberFormatException ex) {
            throw new WeatherDataImporter.ImportException(
                fieldName + " must be a valid number, got: " + value,
                lineNumber,
                ex);
        }
    }

    private LocalDate parseDate(final String value, final int lineNumber)
            throws WeatherDataImporter.ImportException {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (final DateTimeParseException ex) {
            throw new WeatherDataImporter.ImportException(
                "Date must be in format dd/MM/yyyy, got: " + value,
                lineNumber,
                ex);
        }
    }

    private LocalTime parseTime(final String value, final int lineNumber)
            throws WeatherDataImporter.ImportException {
        try {
            return LocalTime.parse(value.trim(), TIME_FORMATTER);
        } catch (final DateTimeParseException ex) {
            throw new WeatherDataImporter.ImportException(
                "Time must be in format HH:mm, got: " + value,
                lineNumber,
                ex);
        }
    }

    @Override
    public String getProviderName() {
        return "CSV File (" + filePath.getFileName() + ")";
    }
}

