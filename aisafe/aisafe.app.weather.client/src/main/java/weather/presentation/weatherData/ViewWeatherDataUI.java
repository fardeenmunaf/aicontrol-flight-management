package weather.presentation.weatherData;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import weather.ClientSession;
import weatherData.application.ListWeatherDataController;
import weatherData.dto.WeatherDataDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ViewWeatherDataUI extends AbstractUI {

    private final ListWeatherDataController controller = new ListWeatherDataController();

    @Override
    protected boolean doShow() {
        System.out.println("\n=== Weather Data Records (filtered) ===");

        final String areaInput = Console.readLine("Enter Air Control Area (ACA) code (leave blank for all): ");
        final String area = (areaInput == null || areaInput.trim().isEmpty()) ? null : areaInput.trim();
        final String dateStr = Console.readLine("Enter Date (dd/MM/yyyy) to filter by (leave blank for all): ");

        Iterable<WeatherDataDTO> records;

        if (ClientSession.getMode() == ClientSession.ConnectionMode.DIRECT_DB) {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                if (area == null) {
                    records = controller.listAll();
                } else {
                    records = controller.listByDateAndArea(area, null);
                }
            } else {
                final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                try {
                    final LocalDate date = LocalDate.parse(dateStr.trim(), fmt);
                    records = controller.listByDateAndArea(area, date);
                } catch (final DateTimeParseException ex) {
                    System.out.println("Invalid date format. Expected dd/MM/yyyy. Aborting.");
                    return false;
                }
            }
        } else {
            // TCP Mode
            try {
                String isoDate = null;
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    isoDate = LocalDate.parse(dateStr.trim(), fmt).toString(); // YYYY-MM-DD
                }
                String response = ClientSession.getClient().listWeather(area, isoDate);
                
                List<WeatherDataDTO> list = new ArrayList<>();
                if (response != null && !response.trim().isEmpty()) {
                    String[] lines = response.split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            list.add(deserializeWeatherDataDTO(line));
                        }
                    }
                }
                records = list;
            } catch (Exception e) {
                System.out.println("Error fetching data from server: " + e.getMessage());
                return false;
            }
        }

        int count = 0;
        for (final WeatherDataDTO record : records) {
            System.out.printf(
                    "ACA: %s | Lat1: %.6f | Long1: %.6f | Lat2: %.6f | Long2: %.6f | " +
                    "Alt_inf: %.0f ft | Alt_sup: %.0f ft | Direction: %.2f° | WindSpeed: %.2f kts | " +
                    "Date: %s | Start: %s | End: %s%n",
                    record.acaCode(),
                    record.latitude1(),
                    record.longitude1(),
                    record.latitude2(),
                    record.longitude2(),
                    record.altitudeInf(),
                    record.altitudeSup(),
                    record.direction(),
                    record.windSpeed(),
                    record.date(),
                    record.startHour(),
                    record.endHour()
            );
            count++;
        }

        if (count == 0) {
            System.out.println("No weather data found for the given filters.");
        } else {
            System.out.println("\nTotal records: " + count);
        }

        return true;
    }

    private WeatherDataDTO deserializeWeatherDataDTO(String line) {
        String[] p = line.split(";", -1);
        return new WeatherDataDTO(
                p[0],
                Double.parseDouble(p[1]),
                Double.parseDouble(p[2]),
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                Double.parseDouble(p[6]),
                Double.parseDouble(p[7]),
                Double.parseDouble(p[8]),
                LocalDate.parse(p[9]),
                LocalTime.parse(p[10]),
                LocalTime.parse(p[11])
        );
    }

    @Override
    public String headline() {
        return "View Weather Data";
    }
}