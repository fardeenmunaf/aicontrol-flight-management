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
 * XML Weather Data Importer (Example of extensibility)
 * Demonstrates how the system can easily expand to new weather data sources.
 *
 * Expected XML format:
 * <weatherDataList>
 *   <weatherData>
 *     <acaCode>SCPA</acaCode>
 *     <latitude1>38.5</latitude1>
 *     <longitude1>-9.0</longitude1>
 *     <latitude2>39.0</latitude2>
 *     <longitude2>-8.5</longitude2>
 *     <altitudeInf>0</altitudeInf>
 *     <altitudeSup>10000</altitudeSup>
 *     <direction>180</direction>
 *     <windSpeed>15.5</windSpeed>
 *     <date>2024-01-15</date>
 *     <startHour>08:00:00</startHour>
 *     <endHour>18:00:00</endHour>
 *   </weatherData>
 * </weatherDataList>
 */
public class XMLWeatherDataImporter implements WeatherDataImporter {

    private final Path filePath;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    public XMLWeatherDataImporter(final String filePath) {
        this.filePath = Path.of(filePath);
    }

    @Override
    public List<WeatherDataDTO> importData() throws WeatherDataImporter.ImportException {
        final List<WeatherDataDTO> weatherDataList = new ArrayList<>();

        if (!Files.exists(filePath)) {
            throw new WeatherDataImporter.ImportException("File not found: " + filePath, 0);
        }

        try {
            final String xmlContent = Files.readString(filePath);

            // Parse XML using simple regex patterns
            // Note: For production, consider using a proper XML parser like DOM or SAX
            final Pattern dataPattern = Pattern.compile("<weatherData>(.*?)</weatherData>", Pattern.DOTALL);
            final Matcher dataMatcher = dataPattern.matcher(xmlContent);

            int lineNumber = 1;
            while (dataMatcher.find()) {
                lineNumber++;
                final String weatherDataXml = dataMatcher.group(1);

                try {
                    final WeatherDataDTO dto = parseWeatherDataElement(weatherDataXml, lineNumber);
                    weatherDataList.add(dto);
                } catch (final WeatherDataImporter.ImportException ex) {
                    throw ex;
                } catch (final Exception ex) {
                    throw new WeatherDataImporter.ImportException(
                        "Error parsing weather data element: " + ex.getMessage(),
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

    private WeatherDataDTO parseWeatherDataElement(final String xml, final int lineNumber)
            throws WeatherDataImporter.ImportException {

        try {
            final String acaCode = extractValue(xml, "acaCode");
            final Double latitude1 = Double.parseDouble(extractValue(xml, "latitude1"));
            final Double longitude1 = Double.parseDouble(extractValue(xml, "longitude1"));
            final Double latitude2 = Double.parseDouble(extractValue(xml, "latitude2"));
            final Double longitude2 = Double.parseDouble(extractValue(xml, "longitude2"));
            final Double altitudeInf = Double.parseDouble(extractValue(xml, "altitudeInf"));
            final Double altitudeSup = Double.parseDouble(extractValue(xml, "altitudeSup"));
            final Double direction = Double.parseDouble(extractValue(xml, "direction"));
            final Double windSpeed = Double.parseDouble(extractValue(xml, "windSpeed"));
            final LocalDate date = LocalDate.parse(extractValue(xml, "date"), DATE_FORMATTER);
            final LocalTime startHour = LocalTime.parse(extractValue(xml, "startHour"), TIME_FORMATTER);
            final LocalTime endHour = LocalTime.parse(extractValue(xml, "endHour"), TIME_FORMATTER);

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

    private String extractValue(final String xml, final String tagName) throws WeatherDataImporter.ImportException {
        final Pattern pattern = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">");
        final Matcher matcher = pattern.matcher(xml);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        throw new WeatherDataImporter.ImportException("Missing tag: " + tagName, 0);
    }

    @Override
    public String getProviderName() {
        return "XML File (" + filePath.getFileName() + ")";
    }
}

