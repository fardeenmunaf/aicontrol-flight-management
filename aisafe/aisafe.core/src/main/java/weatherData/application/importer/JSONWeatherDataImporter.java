package weatherData.application.importer;

import weatherData.dto.WeatherDataDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON Weather Data Importer (Example of extensibility)
 * Demonstrates how the system can easily expand to new weather data sources.
 *
 * Expected JSON format:
 * {
 *   "weatherDataList": [
 *     {
 *       "acaCode": "SCPA",
 *       "latitude1": 38.5,
 *       "longitude1": -9.0,
 *       "latitude2": 39.0,
 *       "longitude2": -8.5,
 *       "altitudeInf": 0,
 *       "altitudeSup": 10000,
 *       "direction": 180,
 *       "windSpeed": 15.5,
 *       "date": "2024-01-15",
 *       "startHour": "08:00:00",
 *       "endHour": "18:00:00"
 *     }
 *   ]
 * }
 */
public class JSONWeatherDataImporter implements WeatherDataImporter {

    private final Path filePath;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    public JSONWeatherDataImporter(final String filePath) {
        this.filePath = Path.of(filePath);
    }

    @Override
    public List<WeatherDataDTO> importData() throws WeatherDataImporter.ImportException {
        final List<WeatherDataDTO> weatherDataList = new ArrayList<>();

        if (!Files.exists(filePath)) {
            throw new WeatherDataImporter.ImportException("File not found: " + filePath, 0);
        }

        try {
            final String jsonContent = Files.readString(filePath);

            // Parse JSON array using simple regex patterns
            // Note: For production, consider using a JSON library like Jackson or Gson
            final Pattern arrayPattern = Pattern.compile("\\{([^{}]*?)\\}", Pattern.DOTALL);
            final Matcher arrayMatcher = arrayPattern.matcher(jsonContent);

            int lineNumber = 1;
            while (arrayMatcher.find()) {
                lineNumber++;
                final String jsonObject = "{" + arrayMatcher.group(1) + "}";

                // Skip the outer wrapper object
                if (jsonObject.contains("weatherDataList")) {
                    continue;
                }

                try {
                    final WeatherDataDTO dto = parseJsonObject(jsonObject, lineNumber);
                    if (dto != null) {
                        weatherDataList.add(dto);
                    }
                } catch (final WeatherDataImporter.ImportException ex) {
                    throw ex;
                } catch (final Exception ex) {
                    throw new WeatherDataImporter.ImportException(
                        "Error parsing JSON object: " + ex.getMessage(),
                        lineNumber,
                        ex);
                }
            }

        } catch (final IOException ex) {
            throw new WeatherDataImporter.ImportException("Error reading file: " + ex.getMessage(), 0, ex);
        } catch (final WeatherDataImporter.ImportException ex) {
            throw ex;
        }

        return weatherDataList;
    }

    private WeatherDataDTO parseJsonObject(final String json, final int lineNumber)
            throws WeatherDataImporter.ImportException {

        try {
            // Check if this is a valid weather data object (has acaCode)
            if (!json.contains("acaCode")) {
                return null;
            }

            final String acaCode = extractJsonValue(json, "acaCode", true);
            final Double latitude1 = Double.parseDouble(extractJsonValue(json, "latitude1", false));
            final Double longitude1 = Double.parseDouble(extractJsonValue(json, "longitude1", false));
            final Double latitude2 = Double.parseDouble(extractJsonValue(json, "latitude2", false));
            final Double longitude2 = Double.parseDouble(extractJsonValue(json, "longitude2", false));
            final Double altitudeInf = Double.parseDouble(extractJsonValue(json, "altitudeInf", false));
            final Double altitudeSup = Double.parseDouble(extractJsonValue(json, "altitudeSup", false));
            final Double direction = Double.parseDouble(extractJsonValue(json, "direction", false));
            final Double windSpeed = Double.parseDouble(extractJsonValue(json, "windSpeed", false));
            final LocalDate date = LocalDate.parse(extractJsonValue(json, "date", true), DATE_FORMATTER);
            final LocalTime startHour = LocalTime.parse(extractJsonValue(json, "startHour", true), TIME_FORMATTER);
            final LocalTime endHour = LocalTime.parse(extractJsonValue(json, "endHour", true), TIME_FORMATTER);

            return new WeatherDataDTO(
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

        } catch (final NumberFormatException | DateTimeParseException ex) {
            throw new WeatherDataImporter.ImportException(
                "Invalid data format: " + ex.getMessage(),
                lineNumber,
                ex);
        }
    }

    private String extractJsonValue(final String json, final String key, final boolean isString)
            throws WeatherDataImporter.ImportException {

        // Pattern for: "key": "value" or "key": value
        final Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*" + (isString ? "\"([^\"]*)\"" : "([\\d\\-.]+)"));
        final Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        throw new WeatherDataImporter.ImportException("Missing or invalid field: " + key, 0);
    }

    @Override
    public String getProviderName() {
        return "JSON File (" + filePath.getFileName() + ")";
    }
}

