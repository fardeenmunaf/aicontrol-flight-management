package weatherData.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import weatherData.domain.WeatherData;
import weatherData.repository.WeatherDataRepository;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Exports all WeatherData records from the database to a JSON file that the
 * C simulation's environment thread (US110) reads at startup.
 *
 * Output format (array of weather area entries):
 * <pre>
 * [
 *   {
 *     "lat1": 38.0, "lon1": -10.0, "lat2": 42.0, "lon2": -6.0,
 *     "altitudeInf": 0.0, "altitudeSup": 12000.0,
 *     "windDirection": 270.0, "windSpeed": 10.0
 *   }
 * ]
 * </pre>
 *
 * The C environment thread matches each active aircraft's position + altitude
 * against the bounding boxes; the first matching entry wins. If no entry
 * matches, the thread falls back to the wind stored in the flight-plan JSON
 * (Segment default values).
 */
public class ExportWeatherDataService {

    private final WeatherDataRepository weatherDataRepository;
    private final ObjectMapper objectMapper;

    public ExportWeatherDataService(final WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = Objects.requireNonNull(weatherDataRepository);
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Exports all WeatherData records to the specified JSON file path.
     *
     * @param outputPath absolute or relative path for the output file
     *                   (e.g. {@code "aisafe-simulation/weather_db.json"})
     * @return number of records exported
     * @throws IOException if the file cannot be written
     */
    public int exportToJson(final String outputPath) throws IOException {
        final ArrayNode array = objectMapper.createArrayNode();

        for (final WeatherData wd : weatherDataRepository.findAll()) {
            final ObjectNode entry = objectMapper.createObjectNode();
            entry.put("lat1",         wd.latitude1());
            entry.put("lon1",         wd.longitude1());
            entry.put("lat2",         wd.latitude2());
            entry.put("lon2",         wd.longitude2());
            entry.put("altitudeInf",  wd.altitudeInf());
            entry.put("altitudeSup",  wd.altitudeSup());
            entry.put("windDirection", wd.direction());
            entry.put("windSpeed",    wd.windSpeed());
            array.add(entry);
        }

        File out = new File(outputPath);
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        objectMapper.writeValue(out, array);
        return array.size();
    }
}
