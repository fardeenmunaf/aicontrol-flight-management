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
 *     "altitudeInf": 0.0, "altitudeSup": 3657.6,
 *     "windDirection": 270.0, "windSpeed": 5.14,
 *     "date": "2026-06-22", "startHour": "05:00", "endHour": "08:15"
 *   }
 * ]
 * </pre>
 *
 * Units are converted to SI for the simulation: altitudes from feet to metres
 * and wind speed from knots to metres-per-second (the C sim treats these values
 * as metres / m·s⁻¹ and does no conversion of its own).
 *
 * The {@code date}/{@code startHour}/{@code endHour} window bounds when the
 * record is active: the C environment thread applies a record's wind only while
 * the simulation wall-clock falls inside it. Without this, every wind record
 * would apply for the entire run.
 *
 * The C environment thread matches each active aircraft's position + altitude
 * (and the current time) against the bounding boxes; the first matching entry
 * wins. If no entry matches, the thread falls back to the wind stored in the
 * flight-plan JSON (Segment default values).
 */
public class ExportWeatherDataService {

    /** Conversion factor: 1 foot = 0.3048 metres. */
    private static final double FEET_TO_METRES = 0.3048;
    /** Conversion factor: 1 knot = 0.514444 metres per second. */
    private static final double KNOTS_TO_MS = 0.514444;

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
            // Convert feet -> metres and knots -> m/s for the SI-based simulation.
            entry.put("altitudeInf",  wd.altitudeInf() * FEET_TO_METRES);
            entry.put("altitudeSup",  wd.altitudeSup() * FEET_TO_METRES);
            entry.put("windDirection", wd.direction());
            entry.put("windSpeed",    wd.windSpeed() * KNOTS_TO_MS);
            // Time window during which this record is active (read by the C sim).
            entry.put("date",         wd.date().toString());        // ISO yyyy-MM-dd
            entry.put("startHour",    wd.startHour().toString());   // HH:mm
            entry.put("endHour",      wd.endHour().toString());     // HH:mm
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
