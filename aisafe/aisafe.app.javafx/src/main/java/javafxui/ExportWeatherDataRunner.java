package javafxui;

import infrastructure.persistence.PersistenceContext;
import weatherData.application.ExportWeatherDataService;

import java.nio.file.Paths;

public class ExportWeatherDataRunner {

    public static void main(String[] args) throws Exception {
        String outputPath = Paths.get("aisafe-simulation/weather_db.json").toAbsolutePath().toString();
        if (args.length > 0) {
            outputPath = args[0];
        }

        ExportWeatherDataService service = new ExportWeatherDataService(
                PersistenceContext.repositories().weatherData());

        int count = service.exportToJson(outputPath);
        System.out.println("Exported " + count + " weather record(s) to " + outputPath);
    }
}
